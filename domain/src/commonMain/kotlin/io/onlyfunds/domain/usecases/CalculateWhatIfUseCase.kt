package io.onlyfunds.domain.usecases

import io.onlyfunds.domain.model.WhatIfResult
import io.onlyfunds.network.NetworkResponse

class CalculateWhatIfUseCase(
    private val getQuote: GetQuoteUseCase = GetQuoteUseCase(),
) {
    suspend operator fun invoke(
        symbol: String,
        buyPrice: Double,
        quantity: Double,
        buyTimestamp: Long,
    ): NetworkResponse<WhatIfResult> =
        when (val response = getQuote(symbol)) {
            is NetworkResponse.Success -> {
                val currentPrice = response.data.current
                val invested = buyPrice * quantity
                val currentValue = currentPrice * quantity
                val profitLoss = currentValue - invested
                val profitLossPercent =
                    if (invested != 0.0) (profitLoss / invested) * 100.0 else 0.0
                NetworkResponse.Success(
                    WhatIfResult(
                        symbol = symbol,
                        quantity = quantity,
                        buyPrice = buyPrice,
                        buyTimestamp = buyTimestamp,
                        currentPrice = currentPrice,
                        invested = invested,
                        currentValue = currentValue,
                        profitLoss = profitLoss,
                        profitLossPercent = profitLossPercent,
                    ),
                    response.statusCode,
                )
            }

            is NetworkResponse.Error -> response
        }
}
