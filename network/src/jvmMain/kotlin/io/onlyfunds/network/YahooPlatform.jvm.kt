package io.onlyfunds.network

internal actual object YahooPlatform {
    actual val userAgent: String? = YahooConfig.USER_AGENT
    actual fun chartUrls(url: String): List<String> = listOf(url)
}
