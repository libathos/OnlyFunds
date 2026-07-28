package io.onlyfunds.domain.mapper

import io.onlyfunds.domain.model.Candle
import io.onlyfunds.domain.model.StockCandles
import io.onlyfunds.network.StockCandles as NetworkStockCandles

fun NetworkStockCandles.toDomain(symbol: String): StockCandles {
    val candles = timestamp.indices.map { i ->
        Candle(
            timestamp = timestamp[i],
            open = open.getOrElse(i) { 0.0 },
            high = high.getOrElse(i) { 0.0 },
            low = low.getOrElse(i) { 0.0 },
            close = close.getOrElse(i) { 0.0 },
            volume = volume.getOrElse(i) { 0.0 },
        )
    }
    return StockCandles(
        symbol = symbol,
        candles = candles,
        hasData = status == "ok" && candles.isNotEmpty(),
    )
}
