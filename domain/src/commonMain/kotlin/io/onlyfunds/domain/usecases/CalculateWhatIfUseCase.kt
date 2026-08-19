package io.onlyfunds.domain.usecases

import io.onlyfunds.domain.model.Candle
import io.onlyfunds.domain.model.WhatIfResult
import io.onlyfunds.network.NetworkResponse

class CalculateWhatIfUseCase(
    private val getQuote: GetQuoteUseCase = GetQuoteUseCase(),
    private val backtestSmaCross: BacktestSmaCrossUseCase = BacktestSmaCrossUseCase(),
) {
    suspend operator fun invoke(
        symbol: String,
        buyPrice: Double,
        quantity: Double,
        buyTimestamp: Long,
        candles: List<Candle> = emptyList(),
        autoTradeOnSmaCross: Boolean = false,
    ): NetworkResponse<WhatIfResult> =
        when (val response = getQuote(symbol)) {
            is NetworkResponse.Success -> {
                val currentPrice = response.data.current
                val invested = buyPrice * quantity
                val currentValue = currentPrice * quantity
                val profitLoss = currentValue - invested
                val profitLossPercent =
                    if (invested != 0.0) (profitLoss / invested) * 100.0 else 0.0
                val smaCross = if (autoTradeOnSmaCross) {
                    backtestSmaCross(
                        symbol = symbol,
                        candles = candles,
                        buyTimestamp = buyTimestamp,
                        buyPrice = buyPrice,
                        quantity = quantity,
                        currentPrice = currentPrice,
                    )
                } else {
                    null
                }
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
                        smaCross = smaCross,
                    ),
                    response.statusCode,
                )
            }

            is NetworkResponse.Error -> response
        }
}
