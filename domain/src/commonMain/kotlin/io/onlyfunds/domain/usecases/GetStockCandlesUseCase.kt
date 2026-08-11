package io.onlyfunds.domain.usecases

import io.onlyfunds.domain.mapper.toDomain
import io.onlyfunds.domain.model.CandleResolution
import io.onlyfunds.domain.model.ChartTimeFrame
import io.onlyfunds.domain.model.StockCandles
import io.onlyfunds.network.CandleService
import io.onlyfunds.network.NetworkResponse
import io.onlyfunds.network.YahooChartService

/**
 * Fetches historical candlestick data for a stock, returning the shared
 * [StockCandles] domain model regardless of which provider served it.
 *
 * Two providers are used: Finnhub `/stock/candle` is the primary source, and
 * Yahoo Finance's free chart endpoint is a fallback for when Finnhub errors
 * (e.g. the endpoint is premium-gated) or returns no data. Finnhub `/quote`
 * remains the source for live prices elsewhere and is not used here.
 *
 * Time math is kept pure: callers pass [nowEpochSeconds] so no clock is read
 * inside the use case.
 *
 * @param candleService Finnhub candle data source.
 * @param yahooChartService Free, no-key fallback candle data source.
 */
class GetStockCandlesUseCase(
    private val candleService: CandleService = CandleService(),
    private val yahooChartService: YahooChartService = YahooChartService(),
) {
    /**
     * Loads candles for an explicit Finnhub [resolution] and `[from, to]` epoch
     * range from Finnhub only (no fallback).
     */
    suspend operator fun invoke(
        symbol: String,
        resolution: CandleResolution,
        from: Long,
        to: Long,
    ): NetworkResponse<StockCandles> =
        when (val response = candleService.getCandles(symbol, resolution.apiValue, from, to)) {
            is NetworkResponse.Success ->
                NetworkResponse.Success(response.data.toDomain(symbol), response.statusCode)

            is NetworkResponse.Error -> response
        }

    /**
     * Loads candles for a [timeFrame], trying Finnhub `/stock/candle` first and
     * falling back to Yahoo Finance's free chart endpoint when Finnhub errors
     * (e.g. the endpoint is premium-gated) or returns no data.
     */
    suspend operator fun invoke(
        symbol: String,
        timeFrame: ChartTimeFrame,
        nowEpochSeconds: Long,
    ): NetworkResponse<StockCandles> {
        val range = timeFrame.toRange(nowEpochSeconds)
        val finnhub = invoke(symbol, timeFrame.resolution, range.from, range.to)
        if (finnhub is NetworkResponse.Success && finnhub.data.hasData) return finnhub

        return when (
            val yahoo = yahooChartService.getChart(
                symbol = symbol.toYahooSymbol(),
                range = timeFrame.yahooRange,
                interval = timeFrame.yahooInterval,
            )
        ) {
            is NetworkResponse.Success ->
                NetworkResponse.Success(yahoo.data.toDomain(symbol), yahoo.statusCode)

            is NetworkResponse.Error -> yahoo
        }
    }

    // Finnhub uses "." for share classes (BRK.A) while Yahoo uses "-" (BRK-A).
    private fun String.toYahooSymbol(): String = replace('.', '-')
}
