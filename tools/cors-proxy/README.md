# OnlyFunds CORS proxy (Cloudflare Worker)

Yahoo Finance's chart endpoint (`query1.finance.yahoo.com/v8/finance/chart/...`)
returns **no `Access-Control-Allow-Origin` header**, so the browser (WASM/JS)
target cannot call it directly. Public CORS proxies (`allorigins.win`,
`corsproxy.io`, …) are unreliable, so this Worker is the supported, self-hosted
way to make the Stock Chart screen load in the browser.

It relays the request to Yahoo, adds a browser `User-Agent` (which Yahoo
requires), returns permissive CORS headers, and caches responses at the edge.
It is **host-allowlisted to Yahoo only**, so it can't be abused as an open proxy.

## Endpoint

```
GET https://<worker-host>/?url=<url-encoded Yahoo URL>
```

Example:

```
https://onlyfunds-cors.<your-subdomain>.workers.dev/?url=https%3A%2F%2Fquery1.finance.yahoo.com%2Fv8%2Ffinance%2Fchart%2FNVR%3Frange%3D1mo%26interval%3D1d
```

## Deploy

Prerequisites: a (free) Cloudflare account and Node.js 18+.

```bash
cd tools/cors-proxy
npm install
npx wrangler login       # opens a browser to authorise
npm run deploy           # prints the deployed https://onlyfunds-cors.<subdomain>.workers.dev URL
```

Local testing:

```bash
npm run dev              # serves on http://localhost:8787
curl "http://localhost:8787/?url=$(python3 -c 'import urllib.parse;print(urllib.parse.quote("https://query1.finance.yahoo.com/v8/finance/chart/NVR?range=1mo&interval=1d"))')"
```

## Point the app at it

Add the deployed URL (ending in `/?url=`) to the repo's git-ignored
`local.properties`:

```properties
yahoo.cors.proxy=https://onlyfunds-cors.<your-subdomain>.workers.dev/?url=
```

Or via an environment variable at build time:

```bash
export YAHOO_CORS_PROXY="https://onlyfunds-cors.<your-subdomain>.workers.dev/?url="
```

Then rebuild the web app:

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

The app tries this proxy **first** (see `YahooConfig.corsProxyUrls`), falling
back to the public proxies only if it is unset or fails.

### URL template

`corsProxyUrls` appends the URL-encoded Yahoo URL to whatever
`yahoo.cors.proxy` you provide. If your proxy needs the URL somewhere other than
the end, include a `{url}` placeholder and it is substituted instead, e.g.:

```properties
yahoo.cors.proxy=https://my-proxy.example.com/fetch?target={url}&cache=1
```
