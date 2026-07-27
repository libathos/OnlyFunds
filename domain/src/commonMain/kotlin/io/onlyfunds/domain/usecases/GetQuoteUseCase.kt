package io.onlyfunds.domain.usecases

import io.onlyfunds.network.NetworkResponse
import io.onlyfunds.network.Quote
import io.onlyfunds.network.QuoteService

class GetQuoteUseCase(
    private val quoteService: QuoteService = QuoteService(),
) {
    suspend operator fun invoke(symbol: String): NetworkResponse<Quote> =
        quoteService.getQuote(symbol)
}