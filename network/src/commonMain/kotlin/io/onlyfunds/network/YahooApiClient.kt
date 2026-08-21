package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

/**
 * Dedicated client for Yahoo Finance's free (no-key) chart endpoint.
 *
 * Kept separate from [FinnhubApiClient] so the Finnhub token is never leaked to
 * Yahoo and so we can attach the browser User-Agent Yahoo requires (where the
 * platform allows setting it - see [YahooPlatform]).
 */
object YahooApiClient {

    val httpClient: HttpClient = HttpClient {
        // Bound every request so a slow/hung CORS proxy on web surfaces an error
        // (and lets the next proxy be tried) instead of spinning forever.
        install(HttpTimeout) {
            requestTimeoutMillis = 8_000
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[YahooApiClient] $message")
                }
            }
            level = LogLevel.INFO
        }
        YahooPlatform.userAgent?.let { agent ->
            defaultRequest {
                header(HttpHeaders.UserAgent, agent)
            }
        }
    }
}
