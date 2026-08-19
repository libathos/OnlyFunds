package io.onlyfunds.domain.model

data class SmaCrossResult(
    val symbol: String,
    val smaPeriod: Int,
    val invested: Double,
    val finalValue: Double,
    val profitLoss: Double,
    val profitLossPercent: Double,
    val buyCount: Int,
    val sellCount: Int,
    val holdingAtEnd: Boolean,
)
