package io.onlyfunds.domain.model

data class PriceAlertStatus(
    val alert: PriceAlert,
    val currentPrice: Double,
    val isTriggered: Boolean,
)
