/**
 * OnlyFunds CORS proxy (Cloudflare Worker).
 *
 * Yahoo Finance's chart endpoint has no `Access-Control-Allow-Origin` header, so
 * browsers (the WASM/JS target) cannot call it directly. This Worker relays the
 * request and adds permissive CORS headers, a browser `User-Agent` (which Yahoo
 * requires), and edge caching.
 *
 * Usage: GET https://<worker-host>/?url=<url-encoded target>
 *
 * It is host-allowlisted (Yahoo only) so it can't be abused as an open proxy.
 * Point the app at it via `yahoo.cors.proxy=https://<worker-host>/?url=`
 * in local.properties (see network/build.gradle.kts).
 */

export interface Env {
  /** Comma-separated host allowlist. Defaults to Yahoo's chart hosts. */
  ALLOWED_HOSTS?: string;
}

const DEFAULT_ALLOWED_HOSTS = [
  "query1.finance.yahoo.com",
  "query2.finance.yahoo.com",
];

const BROWSER_USER_AGENT =
  "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
  "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, OPTIONS",
  "Access-Control-Allow-Headers": "*",
  "Access-Control-Max-Age": "86400",
};

function jsonError(message: string, status: number): Response {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: { ...CORS_HEADERS, "Content-Type": "application/json" },
  });
}

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: CORS_HEADERS });
    }
    if (request.method !== "GET") {
      return jsonError("Method not allowed", 405);
    }

    const target = new URL(request.url).searchParams.get("url");
    if (!target) {
      return jsonError("Missing 'url' query parameter", 400);
    }

    let parsed: URL;
    try {
      parsed = new URL(target);
    } catch {
      return jsonError("Invalid target URL", 400);
    }

    const allowed =
      env.ALLOWED_HOSTS?.split(",").map((h) => h.trim()) ??
      DEFAULT_ALLOWED_HOSTS;
    if (!allowed.includes(parsed.hostname)) {
      return jsonError(`Host not allowed: ${parsed.hostname}`, 403);
    }

    const upstream = await fetch(parsed.toString(), {
      headers: {
        "User-Agent": BROWSER_USER_AGENT,
        Accept: "application/json",
      },
      cf: {
        cacheTtl: 300, // cache historical candle data for 5 minutes at the edge
        cacheEverything: true,
      },
    });

    const body = await upstream.text();
    return new Response(body, {
      status: upstream.status,
      headers: {
        ...CORS_HEADERS,
        "Content-Type": "application/json; charset=utf-8",
        "Cache-Control": "public, max-age=300",
      },
    });
  },
};
