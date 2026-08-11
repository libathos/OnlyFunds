package compose.demo.onlyfunds

import io.onlyfunds.domain.model.Quote

data class StockQuote(val symbol: String, val quote: Quote)

sealed interface TopStocksMutation {
    data object ShowLoading : TopStocksMutation
    data object StartRefresh : TopStocksMutation
    data object StopRefresh : TopStocksMutation
    data class ShowStocks(val stocks: List<StockQuote>) : TopStocksMutation
    data class ShowError(val message: String) : TopStocksMutation
}

class TopStocksUiProvider(
    private val title: String = "Top 10 Most Expensive Stocks",
    private val refreshIntervalMillis: Long = 15_000L,
) {
    fun initialState(): TopStocksUiState =
        TopStocksUiState(
            title = title,
            refreshIntervalMillis = refreshIntervalMillis,
            content = TopStocksUiState.Content.Loading,
        )

    fun reduce(current: TopStocksUiState, mutation: TopStocksMutation): TopStocksUiState =
        when (mutation) {
            TopStocksMutation.ShowLoading ->
                current.copy(isRefreshing = false, content = TopStocksUiState.Content.Loading)

            TopStocksMutation.StartRefresh ->
                current.copy(isRefreshing = true)

            TopStocksMutation.StopRefresh ->
                current.copy(isRefreshing = false)

            is TopStocksMutation.ShowError ->
                current.copy(
                    isRefreshing = false,
                    content = TopStocksUiState.Content.Error(mutation.message),
                )

            is TopStocksMutation.ShowStocks -> {
                val previousPrices = (current.content as? TopStocksUiState.Content.Stocks)
                    ?.rows
                    ?.associate { it.symbol to it.priceValue }
                    .orEmpty()
                current.copy(
                    isRefreshing = false,
                    refreshToken = current.refreshToken + 1,
                    content = TopStocksUiState.Content.Stocks(
                        rows = mutation.stocks.mapIndexed { index, stock ->
                            stock.toUiModel(
                                rank = index + 1,
                                previousPrice = previousPrices[stock.symbol],
                            )
                        },
                    ),
                )
            }
        }

    private fun StockQuote.toUiModel(rank: Int, previousPrice: Double?): StockRowUiModel {
        val trend = when {
            previousPrice == null -> PriceTrend.Flat
            quote.current > previousPrice -> PriceTrend.Up
            quote.current < previousPrice -> PriceTrend.Down
            else -> PriceTrend.Flat
        }
        return StockRowUiModel(
            rank = rank.toString(),
            symbol = symbol,
            price = formatUsd(quote.current),
            priceValue = quote.current,
            trend = trend,
        )
    }
}
