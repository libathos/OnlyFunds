package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.path

class YahooChartService(
    private val httpClient: HttpClient = YahooApiClient.httpClient,
) {
    suspend fun getChart(
        symbol: String,
        range: String,
        interval: String,
    ): NetworkResponse<YahooChartResponse> {
        return try {
            val response = httpClient.get {
                url {
                    protocol = URLProtocol.HTTPS
                    host = YahooConfig.HOST
                    path(*(YahooConfig.CHART_PATH_SEGMENTS + symbol).toTypedArray())
                    parameters.append(YahooConfig.RANGE_PARAM, range)
                    parameters.append(YahooConfig.INTERVAL_PARAM, interval)
                }
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
