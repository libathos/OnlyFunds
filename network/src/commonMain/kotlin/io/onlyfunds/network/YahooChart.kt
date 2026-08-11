package io.onlyfunds.network

import kotlinx.serialization.Serializable

/**
 * DTOs for Yahoo Finance's `/v8/finance/chart/{symbol}` response.
 *
 * Prices come back as parallel arrays that may contain `null` for gaps
 * (holidays, halted sessions), so numeric lists are nullable per-element.
 */
@Serializable
data class YahooChartResponse(
    val chart: YahooChart = YahooChart(),
)

@Serializable
data class YahooChart(
    val result: List<YahooChartResult>? = null,
    val error: YahooChartError? = null,
)

@Serializable
data class YahooChartError(
    val code: String? = null,
    val description: String? = null,
)

@Serializable
data class YahooChartResult(
    val meta: YahooChartMeta? = null,
    val timestamp: List<Long> = emptyList(),
    val indicators: YahooChartIndicators = YahooChartIndicators(),
)

@Serializable
data class YahooChartMeta(
    val symbol: String? = null,
    val currency: String? = null,
)

@Serializable
data class YahooChartIndicators(
    val quote: List<YahooChartQuote> = emptyList(),
)

@Serializable
data class YahooChartQuote(
    val open: List<Double?> = emptyList(),
    val high: List<Double?> = emptyList(),
    val low: List<Double?> = emptyList(),
    val close: List<Double?> = emptyList(),
    val volume: List<Long?> = emptyList(),
)
