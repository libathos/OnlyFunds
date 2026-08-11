package compose.demo.onlyfunds

import io.onlyfunds.domain.model.ChartTimeFrame

data class ChartPoint(val timestamp: Long, val price: Double)

data class StockChartUiState(
    val symbol: String,
    val selectedTimeFrame: ChartTimeFrame,
    val content: Content,
) {
    sealed interface Content {
        data object Loading : Content
        data class Error(val message: String) : Content
        data class Chart(
            val points: List<ChartPoint>,
            val latestPrice: String,
            val changeLabel: String,
            val trend: PriceTrend,
            val minLabel: String,
            val maxLabel: String,
        ) : Content
    }
}
