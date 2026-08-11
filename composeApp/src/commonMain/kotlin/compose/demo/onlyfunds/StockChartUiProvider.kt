package compose.demo.onlyfunds

import io.onlyfunds.domain.model.ChartTimeFrame
import io.onlyfunds.domain.model.StockCandles

sealed interface StockChartMutation {
    data object ShowLoading : StockChartMutation
    data class SelectTimeFrame(val timeFrame: ChartTimeFrame) : StockChartMutation
    data class ShowChart(val candles: StockCandles) : StockChartMutation
    data class ShowError(val message: String) : StockChartMutation
}

class StockChartUiProvider {
    fun initialState(symbol: String, timeFrame: ChartTimeFrame): StockChartUiState =
        StockChartUiState(
            symbol = symbol,
            selectedTimeFrame = timeFrame,
            content = StockChartUiState.Content.Loading,
        )

    fun reduce(current: StockChartUiState, mutation: StockChartMutation): StockChartUiState =
        when (mutation) {
            StockChartMutation.ShowLoading ->
                current.copy(content = StockChartUiState.Content.Loading)

            is StockChartMutation.SelectTimeFrame ->
                current.copy(
                    selectedTimeFrame = mutation.timeFrame,
                    content = StockChartUiState.Content.Loading,
                )

            is StockChartMutation.ShowError ->
                current.copy(content = StockChartUiState.Content.Error(mutation.message))

            is StockChartMutation.ShowChart ->
                current.copy(content = toChartContent(mutation.candles))
        }

    private fun toChartContent(candles: StockCandles): StockChartUiState.Content {
        val points = candles.candles.map { ChartPoint(it.timestamp, it.close) }
        if (!candles.hasData || points.isEmpty()) {
            return StockChartUiState.Content.Error("No chart data available for this period.")
        }

        val prices = points.map { it.price }
        val first = prices.first()
        val last = prices.last()
        val change = last - first
        val percent = if (first != 0.0) change / first * 100 else 0.0
        val trend = when {
            change > 0 -> PriceTrend.Up
            change < 0 -> PriceTrend.Down
            else -> PriceTrend.Flat
        }

        return StockChartUiState.Content.Chart(
            points = points,
            latestPrice = formatUsd(last),
            changeLabel = "${formatSignedUsd(change)} (${formatSignedPercent(percent)})",
            trend = trend,
            minLabel = formatUsd(prices.min()),
            maxLabel = formatUsd(prices.max()),
        )
    }
}
