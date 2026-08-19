package compose.demo.onlyfunds.stockChartScreen.mvi

import compose.demo.onlyfunds.topStocksScreen.mvi.PriceTrend
import io.onlyfunds.domain.model.ChartTimeFrame

data class ChartPoint(val timestamp: Long, val price: Double)

data class SmaCrossUiModel(
    val strategyLabel: String,
    val finalValueLabel: String,
    val profitLossLabel: String,
    val profitLossPercentLabel: String,
    val tradesLabel: String,
    val positionLabel: String,
    val trend: PriceTrend,
    val verdict: String,
)

data class WhatIfUiModel(
    val symbol: String,
    val quantityLabel: String,
    val buyDateLabel: String,
    val buyPriceLabel: String,
    val currentPriceLabel: String,
    val investedLabel: String,
    val currentValueLabel: String,
    val profitLossLabel: String,
    val profitLossPercentLabel: String,
    val trend: PriceTrend,
    val smaCross: SmaCrossUiModel? = null,
)

sealed interface WhatIfState {
    data object Calculating : WhatIfState
    data class Ready(val model: WhatIfUiModel) : WhatIfState
    data class Error(val message: String) : WhatIfState
}

data class StockChartUiState(
    val symbol: String,
    val selectedTimeFrame: ChartTimeFrame,
    val content: Content,
    val whatIf: WhatIfState? = null,
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
            val smaValues: List<Double?> = emptyList(),
            val smaLabel: String = "",
        ) : Content
    }
}
