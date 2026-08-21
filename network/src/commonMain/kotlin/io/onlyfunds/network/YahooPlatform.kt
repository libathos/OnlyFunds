package io.onlyfunds.network

/**
 * Platform tweaks required to reach Yahoo's chart endpoint.
 *
 * Browsers make the plain call impossible: Yahoo answers without any
 * `Access-Control-Allow-Origin` header, and a page is not allowed to set
 * `User-Agent` on a request. Web therefore relays the call through a
 * CORS-enabled proxy and sends no custom User-Agent, while every other
 * platform calls Yahoo directly.
 */
internal expect object YahooPlatform {

    /** User-Agent to send, or `null` when the platform forbids setting it. */
    val userAgent: String?

    /**
     * Candidate URLs to request for a given direct Yahoo [url], in priority
     * order. Native platforms return the direct URL; web returns proxied URLs
     * (Yahoo has no CORS headers) and lists several so a failing proxy can fall
     * back to the next.
     */
    fun chartUrls(url: String): List<String>
}
