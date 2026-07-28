package io.onlyfunds.domain.usecases

import io.onlyfunds.domain.model.AlertDirection
import io.onlyfunds.domain.model.PriceAlert
import io.onlyfunds.domain.model.PriceAlertStatus
import io.onlyfunds.network.NetworkResponse

class EvaluatePriceAlertUseCase(
    private val getQuote: GetQuoteUseCase = GetQuoteUseCase(),
) {
    suspend operator fun invoke(alert: PriceAlert): NetworkResponse<PriceAlertStatus> =
        when (val response = getQuote(alert.symbol)) {
            is NetworkResponse.Success -> {
                val currentPrice = response.data.current
                val isTriggered = when (alert.direction) {
                    AlertDirection.ABOVE -> currentPrice >= alert.targetPrice
                    AlertDirection.BELOW -> currentPrice <= alert.targetPrice
                }
                NetworkResponse.Success(
                    PriceAlertStatus(alert, currentPrice, isTriggered),
                    response.statusCode,
                )
            }

            is NetworkResponse.Error -> response
        }
}
