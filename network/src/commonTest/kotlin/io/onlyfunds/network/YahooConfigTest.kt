package io.onlyfunds.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class YahooConfigTest {

    private val directUrl =
        "https://query1.finance.yahoo.com/v8/finance/chart/NVR?range=1mo&interval=1d"

    @Test
    fun corsProxyUrls_returnsPublicFallbacksWhenNoCustomProxy() {
        // Default build has no custom proxy configured, so only the public
        // fallbacks are returned.
        if (YahooSecrets.CORS_PROXY.isNotBlank()) return

        val urls = YahooConfig.corsProxyUrls(directUrl)

        assertEquals(4, urls.size)
        assertTrue(urls.all { it != directUrl }, "web must never hit Yahoo directly")
        assertTrue(
            urls.any { it.contains("corsproxy.io") },
            "expected corsproxy.io among fallbacks",
        )
        // The target URL is carried through each proxy; query-param proxies
        // receive it URL-encoded (range=1mo -> range%3D1mo).
        assertTrue(
            urls.first().contains("range%3D1mo"),
            "encoded target query must be carried through the proxy URL",
        )
    }

    @Test
    fun corsProxyUrls_prependsCustomProxyWhenConfigured() {
        // Only meaningful when a custom proxy is configured at build time
        // (yahoo.cors.proxy / YAHOO_CORS_PROXY).
        val custom = YahooSecrets.CORS_PROXY
        if (custom.isBlank()) return

        val urls = YahooConfig.corsProxyUrls(directUrl)

        assertEquals(5, urls.size, "custom proxy + 4 public fallbacks")
        assertTrue(
            urls.first().startsWith(custom.substringBefore("{url}")),
            "custom proxy must be tried first",
        )
        assertTrue(
            urls.first().contains("range%3D1mo"),
            "encoded target query must be carried through the custom proxy URL",
        )
    }
}
