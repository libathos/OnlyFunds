package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class QuoteService(
    private val httpClient: HttpClient = FinnhubApiClient.httpClient,
) {
    suspend fun getQuote(symbol: String): NetworkResponse<Quote> {
        return try {
            val response = httpClient.get(FinnhubConfig.QUOTE_PATH) {
                parameter(FinnhubConfig.SYMBOL_PARAM, symbol)
            }
            if (response.status.isSuccess()) {
                NetworkResponse.Success(response.body(), response.status.value)
            } else {
                NetworkResponse.Error(response.status.value, response.status.description)
            }
        } catch (e: Exception) {
            NetworkResponse.Error(statusCode = -1, message = e.message ?: "Unknown network error")
        }
    }
}
