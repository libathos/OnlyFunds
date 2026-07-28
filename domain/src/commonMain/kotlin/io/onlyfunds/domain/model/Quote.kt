package io.onlyfunds.domain.model

data class Quote(
    val current: Double,
    val change: Double?,
    val percentChange: Double?,
    val high: Double,
    val low: Double,
    val open: Double,
    val previousClose: Double,
    val timestamp: Long,
)
