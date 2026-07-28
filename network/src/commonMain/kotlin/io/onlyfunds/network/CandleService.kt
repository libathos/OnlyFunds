package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class CandleService(
    private val httpClient: HttpClient = FinnhubApiClient.httpClient,
) {
    suspend fun getCandles(
        symbol: String,
        resolution: String,
        from: Long,
        to: Long,
    ): NetworkResponse<StockCandles> {
        return try {
            val response = httpClient.get(FinnhubConfig.CANDLE_PATH) {
                parameter(FinnhubConfig.SYMBOL_PARAM, symbol)
                parameter(FinnhubConfig.RESOLUTION_PARAM, resolution)
                parameter(FinnhubConfig.FROM_PARAM, from)
                parameter(FinnhubConfig.TO_PARAM, to)
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
