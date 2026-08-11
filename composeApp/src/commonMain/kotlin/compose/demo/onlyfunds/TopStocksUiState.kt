package compose.demo.onlyfunds

enum class PriceTrend { Up, Down, Flat }

data class StockRowUiModel(
    val rank: String,
    val symbol: String,
    val price: String,
    val priceValue: Double,
    val trend: PriceTrend,
)

data class TopStocksUiState(
    val title: String,
    val isRefreshing: Boolean = false,
    val refreshIntervalMillis: Long = 15_000L,
    val refreshToken: Int = 0,
    val content: Content,
) {
    sealed interface Content {
        data object Loading : Content
        data class Error(val message: String) : Content
        data class Stocks(val rows: List<StockRowUiModel>) : Content
    }
}
