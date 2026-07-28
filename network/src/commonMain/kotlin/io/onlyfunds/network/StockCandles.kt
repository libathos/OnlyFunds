package io.onlyfunds.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StockCandles(
    @SerialName("o") val open: List<Double> = emptyList(),
    @SerialName("h") val high: List<Double> = emptyList(),
    @SerialName("l") val low: List<Double> = emptyList(),
    @SerialName("c") val close: List<Double> = emptyList(),
    @SerialName("v") val volume: List<Double> = emptyList(),
    @SerialName("t") val timestamp: List<Long> = emptyList(),
    @SerialName("s") val status: String,
)
