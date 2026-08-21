package io.onlyfunds.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.path
import kotlinx.serialization.json.Json

class YahooChartService(
    private val httpClient: HttpClient = YahooApiClient.httpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getChart(
        symbol: String,
        range: String,
        interval: String,
    ): NetworkResponse<YahooChartResponse> {
        val directUrl = URLBuilder().apply {
            protocol = URLProtocol.HTTPS
            host = YahooConfig.HOST
            path(*(YahooConfig.CHART_PATH_SEGMENTS + symbol).toTypedArray())
            parameters.append(YahooConfig.RANGE_PARAM, range)
            parameters.append(YahooConfig.INTERVAL_PARAM, interval)
        }.buildString()

        // On web each candidate is a different CORS proxy; on native there is a
        // single direct URL. Try them in order until one yields a usable chart
        // so a failing/hanging proxy falls back to the next instead of leaving
        // the UI stuck on a spinner.
        var lastError = NetworkResponse.Error(statusCode = -1, message = "No chart source responded")
        for (candidate in YahooPlatform.chartUrls(directUrl)) {
            try {
                val response = httpClient.get(candidate)
                if (!response.status.isSuccess()) {
                    lastError = NetworkResponse.Error(response.status.value, response.status.description)
                    continue
                }
                // Proxies answer with `text/plain`, so parse by hand instead of
                // via content negotiation.
                val chart = json.decodeFromString<YahooChartResponse>(response.bodyAsText())
                val hasData = chart.chart.result?.firstOrNull()?.timestamp?.isNotEmpty() == true
                if (hasData) {
                    return NetworkResponse.Success(chart, response.status.value)
                }
                // A proxy relaying an error page can still decode to an empty
                // chart, so treat that as a failure and try the next source.
                lastError = NetworkResponse.Error(response.status.value, "Empty chart payload")
            } catch (e: Exception) {
                lastError = NetworkResponse.Error(statusCode = -1, message = e.message ?: "Unknown network error")
            }
        }
        return lastError
    }
}
