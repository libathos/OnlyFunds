package io.onlyfunds.domain.mapper

import io.onlyfunds.domain.model.Candle
import io.onlyfunds.domain.model.StockCandles
import io.onlyfunds.network.YahooChartResponse

/**
 * Maps a Yahoo Finance chart response into the shared [StockCandles] domain
 * model, so the chart UI is agnostic to whichever provider served the data.
 *
 * Indices where the close price is `null` (session gaps) are skipped; missing
 * open/high/low fall back to the close so a partial candle is still drawable.
 */
fun YahooChartResponse.toDomain(symbol: String): StockCandles {
    val result = chart.result?.firstOrNull()
    val timestamps = result?.timestamp ?: emptyList()
    val quote = result?.indicators?.quote?.firstOrNull()

    val candles = timestamps.indices.mapNotNull { i ->
        val close = quote?.close?.getOrNull(i) ?: return@mapNotNull null
        Candle(
            timestamp = timestamps[i],
            open = quote.open.getOrNull(i) ?: close,
            high = quote.high.getOrNull(i) ?: close,
            low = quote.low.getOrNull(i) ?: close,
            close = close,
            volume = quote.volume.getOrNull(i)?.toDouble() ?: 0.0,
        )
    }

    return StockCandles(
        symbol = symbol,
        candles = candles,
        hasData = chart.error == null && candles.isNotEmpty(),
    )
}
