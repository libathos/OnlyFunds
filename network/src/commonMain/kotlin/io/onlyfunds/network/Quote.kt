package io.onlyfunds.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Quote(
    @SerialName("c") val current: Double,
    @SerialName("d") val change: Double? = null,
    @SerialName("dp") val percentChange: Double? = null,
    @SerialName("h") val high: Double,
    @SerialName("l") val low: Double,
    @SerialName("o") val open: Double,
    @SerialName("pc") val previousClose: Double,
    @SerialName("t") val timestamp: Long,
)
