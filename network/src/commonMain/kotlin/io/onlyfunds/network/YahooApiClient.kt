package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Dedicated client for Yahoo Finance's free (no-key) chart endpoint.
 *
 * Kept separate from [FinnhubApiClient] so the Finnhub token is never leaked to
 * Yahoo and so we can attach the browser User-Agent Yahoo requires.
 */
object YahooApiClient {

    val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[YahooApiClient] $message")
                }
            }
            level = LogLevel.INFO
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, YahooConfig.USER_AGENT)
        }
    }
}
