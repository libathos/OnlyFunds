package io.onlyfunds.network

import io.ktor.http.encodeURLParameter

object YahooConfig {
    const val HOST: String = "query1.finance.yahoo.com"
    const val RANGE_PARAM: String = "range"
    const val INTERVAL_PARAM: String = "interval"

    // Yahoo's public chart endpoint rejects requests without a browser-like
    // User-Agent (HTTP 429), so we send one on every call.
    const val USER_AGENT: String =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val CHART_PATH_SEGMENTS: List<String> = listOf("v8", "finance", "chart")

    // Browsers cannot call Yahoo directly (no `Access-Control-Allow-Origin`
    // header on the response), so web targets relay the request through a CORS
    // proxy that echoes the payload with `Access-Control-Allow-Origin: *`.
    //
    // A self-hosted proxy configured via `YahooSecrets.CORS_PROXY` (see
    // network/build.gradle.kts) is tried first when present, since public
    // proxies are flaky (rate limits, downtime). The public list is an ordered
    // best-effort fallback, each tried in turn until one returns a usable payload.
    fun corsProxyUrls(directUrl: String): List<String> {
        val encoded = directUrl.encodeURLParameter()
        val custom = YahooSecrets.CORS_PROXY.takeIf { it.isNotBlank() }?.let { base ->
            if (base.contains("{url}")) base.replace("{url}", encoded) else base + encoded
        }
        val public = listOf(
            "https://corsproxy.io/?url=$encoded",
            "https://api.codetabs.com/v1/proxy/?quest=$encoded",
            "https://api.allorigins.win/raw?url=$encoded",
            "https://thingproxy.freeboard.io/fetch/$directUrl",
        )
        return listOfNotNull(custom) + public
    }
}
