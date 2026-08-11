package io.onlyfunds.network

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
}
