package compose.demo.onlyfunds.stockChartScreen.mvi

import compose.demo.onlyfunds.application.misc.formatDate
import compose.demo.onlyfunds.application.misc.formatQuantity
import compose.demo.onlyfunds.application.misc.formatSignedPercent
import compose.demo.onlyfunds.application.misc.formatSignedUsd
import compose.demo.onlyfunds.application.misc.formatUsd
import compose.demo.onlyfunds.topStocksScreen.mvi.PriceTrend
import io.onlyfunds.domain.model.ChartTimeFrame
import io.onlyfunds.domain.model.StockCandles
import io.onlyfunds.domain.model.WhatIfResult

sealed interface StockChartMutation {
    data object ShowLoading : StockChartMutation
    data class SelectTimeFrame(val timeFrame: ChartTimeFrame) : StockChartMutation
    data class ShowChart(val candles: StockCandles) : StockChartMutation
    data class ShowError(val message: String) : StockChartMutation
    data object ShowWhatIfLoading : StockChartMutation
    data class ShowWhatIfResult(val result: WhatIfResult) : StockChartMutation
    data class ShowWhatIfError(val message: String) : StockChartMutation
    data object DismissWhatIf : StockChartMutation
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

            StockChartMutation.ShowWhatIfLoading ->
                current.copy(whatIf = WhatIfState.Calculating)

            is StockChartMutation.ShowWhatIfResult ->
                current.copy(whatIf = WhatIfState.Ready(mutation.result.toWhatIfUiModel()))

            is StockChartMutation.ShowWhatIfError ->
                current.copy(whatIf = WhatIfState.Error(mutation.message))

            StockChartMutation.DismissWhatIf ->
                current.copy(whatIf = null)
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

    private fun WhatIfResult.toWhatIfUiModel(): WhatIfUiModel {
        val trend = when {
            profitLoss > 0 -> PriceTrend.Up
            profitLoss < 0 -> PriceTrend.Down
            else -> PriceTrend.Flat
        }
        return WhatIfUiModel(
            symbol = symbol,
            quantityLabel = formatQuantity(quantity),
            buyDateLabel = formatDate(buyTimestamp),
            buyPriceLabel = formatUsd(buyPrice),
            currentPriceLabel = formatUsd(currentPrice),
            investedLabel = formatUsd(invested),
            currentValueLabel = formatUsd(currentValue),
            profitLossLabel = formatSignedUsd(profitLoss),
            profitLossPercentLabel = formatSignedPercent(profitLossPercent),
            trend = trend,
        )
    }
}
