package io.onlyfunds.domain.model

data class StockCandles(
    val symbol: String,
    val candles: List<Candle>,
    val hasData: Boolean,
)
