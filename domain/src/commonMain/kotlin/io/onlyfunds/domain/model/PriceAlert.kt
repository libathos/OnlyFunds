package io.onlyfunds.domain.model

enum class AlertDirection { ABOVE, BELOW }

data class PriceAlert(
    val symbol: String,
    val targetPrice: Double,
    val direction: AlertDirection,
)
