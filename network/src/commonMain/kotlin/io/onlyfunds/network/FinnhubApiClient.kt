package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object FinnhubApiClient {

    val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("[FinnhubApiClient] $message")
                }
            }
            level = LogLevel.INFO
        }
        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = FinnhubConfig.HOST
                path("api", "v1", "")
                parameters.append(FinnhubConfig.TOKEN_PARAM, FinnhubSecrets.API_KEY)
            }
        }
    }
}
