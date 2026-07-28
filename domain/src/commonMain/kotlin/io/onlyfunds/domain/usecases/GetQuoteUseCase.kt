package io.onlyfunds.domain.usecases

import io.onlyfunds.domain.mapper.toDomain
import io.onlyfunds.domain.model.Quote
import io.onlyfunds.network.NetworkResponse
import io.onlyfunds.network.QuoteService

class GetQuoteUseCase(
    private val quoteService: QuoteService = QuoteService(),
) {
    suspend operator fun invoke(symbol: String): NetworkResponse<Quote> =
        when (val response = quoteService.getQuote(symbol)) {
            is NetworkResponse.Success ->
                NetworkResponse.Success(response.data.toDomain(), response.statusCode)

            is NetworkResponse.Error -> response
        }
}