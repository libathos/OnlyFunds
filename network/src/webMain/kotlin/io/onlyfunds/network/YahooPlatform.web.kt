package io.onlyfunds.network

internal actual object YahooPlatform {

    // Browsers refuse to let a page set User-Agent, so we never send one.
    actual val userAgent: String? = null

    // Yahoo replies without CORS headers, so the browser can only reach it
    // through a proxy that adds them. Several are tried in turn since public
    // proxies are unreliable.
    actual fun chartUrls(url: String): List<String> = YahooConfig.corsProxyUrls(url)
}
