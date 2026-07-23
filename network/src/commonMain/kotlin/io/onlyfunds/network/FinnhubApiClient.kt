package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.http.path

object FinnhubApiClient {

    val httpClient: HttpClient = HttpClient {
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
