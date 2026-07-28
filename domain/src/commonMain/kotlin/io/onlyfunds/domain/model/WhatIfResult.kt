package io.onlyfunds.domain.model

data class WhatIfResult(
    val symbol: String,
    val quantity: Double,
    val buyPrice: Double,
    val buyTimestamp: Long,
    val currentPrice: Double,
    val invested: Double,
    val currentValue: Double,
    val profitLoss: Double,
    val profitLossPercent: Double,
)
