package compose.demo.onlyfunds

import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartMutation
import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartUiProvider
import compose.demo.onlyfunds.stockChartScreen.mvi.WhatIfState
import compose.demo.onlyfunds.topStocksScreen.mvi.PriceTrend
import io.onlyfunds.domain.model.Candle
import io.onlyfunds.domain.model.ChartTimeFrame
import io.onlyfunds.domain.model.SmaCrossResult
import io.onlyfunds.domain.model.WhatIfResult
import io.onlyfunds.domain.usecases.BacktestSmaCrossUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmaCrossBacktestTest {

    private val backtest = BacktestSmaCrossUseCase()

    private fun candlesOf(closes: List<Double>): List<Candle> =
        closes.mapIndexed { index, close ->
            Candle(
                timestamp = (index + 1) * 86_400L,
                open = close,
                high = close,
                low = close,
                close = close,
                volume = 0.0,
            )
        }

    @Test
    fun testRisingTrendBuysOnceAndKeepsHolding() {
        val candles = candlesOf(listOf(10.0, 12.0, 14.0, 16.0, 18.0, 20.0))
        val result = backtest(
            symbol = "AAPL",
            candles = candles,
            buyTimestamp = candles.first().timestamp,
            buyPrice = 10.0,
            quantity = 10.0,
            currentPrice = 22.0,
        )

        assertNotNull(result)
        assertEquals(3, result.smaPeriod)
        assertEquals(1, result.buyCount)
        assertEquals(0, result.sellCount)
        assertTrue(result.holdingAtEnd)
        assertEquals(100.0, result.invested, 1e-9)
        // Entered at the first close above the SMA (14.0) with the whole $100.
        assertEquals(100.0 / 14.0 * 22.0, result.finalValue, 1e-9)
        assertEquals(result.finalValue - 100.0, result.profitLoss, 1e-9)
    }

    @Test
    fun testCloseBelowAverageSellsThePosition() {
        val candles = candlesOf(listOf(10.0, 12.0, 14.0, 16.0, 10.0, 6.0))
        val result = backtest(
            symbol = "AAPL",
            candles = candles,
            buyTimestamp = candles.first().timestamp,
            buyPrice = 10.0,
            quantity = 10.0,
            currentPrice = 6.0,
        )

        assertNotNull(result)
        assertEquals(1, result.buyCount)
        assertEquals(1, result.sellCount)
        assertFalse(result.holdingAtEnd)
        // Entered at 14.0, left the market on the 10.0 close before the slide to 6.0.
        assertEquals(100.0 / 14.0 * 10.0, result.finalValue, 1e-9)
        // Selling on the cross beats staying invested all the way down.
        assertTrue(result.profitLoss > (10.0 * 6.0) - 100.0)
    }

    @Test
    fun testSignalsBeforeTheBuyDateAreIgnored() {
        val candles = candlesOf(listOf(10.0, 12.0, 14.0, 16.0, 18.0, 20.0))
        val result = backtest(
            symbol = "AAPL",
            candles = candles,
            buyTimestamp = candles[4].timestamp,
            buyPrice = 18.0,
            quantity = 10.0,
            currentPrice = 22.0,
        )

        assertNotNull(result)
        assertEquals(1, result.buyCount)
        assertEquals(180.0, result.invested, 1e-9)
        assertEquals(180.0 / 18.0 * 22.0, result.finalValue, 1e-9)
    }

    @Test
    fun testTooFewCandlesProducesNoResult() {
        val result = backtest(
            symbol = "AAPL",
            candles = candlesOf(listOf(10.0, 11.0, 12.0)),
            buyTimestamp = 86_400L,
            buyPrice = 10.0,
            quantity = 1.0,
            currentPrice = 12.0,
        )

        assertNull(result)
    }

    @Test
    fun testUnknownBuyTimestampProducesNoResult() {
        val candles = candlesOf(listOf(10.0, 11.0, 12.0, 13.0))
        val result = backtest(
            symbol = "AAPL",
            candles = candles,
            buyTimestamp = candles.last().timestamp + 86_400L,
            buyPrice = 10.0,
            quantity = 1.0,
            currentPrice = 12.0,
        )

        assertNull(result)
    }

    @Test
    fun testProviderMapsWinningSmaCrossStrategy() {
        val provider = StockChartUiProvider()
        val state = provider.initialState("AAPL", ChartTimeFrame.MONTH)

        val result = provider.reduce(
            state,
            StockChartMutation.ShowWhatIfResult(
                whatIfResult(
                    profitLoss = 20.0,
                    smaCross = SmaCrossResult(
                        symbol = "AAPL",
                        smaPeriod = 20,
                        invested = 100.0,
                        finalValue = 150.0,
                        profitLoss = 50.0,
                        profitLossPercent = 50.0,
                        buyCount = 1,
                        sellCount = 2,
                        holdingAtEnd = false,
                    ),
                ),
            ),
        )

        val model = (result.whatIf as WhatIfState.Ready).model
        val smaCross = assertNotNull(model.smaCross)
        assertEquals("Auto trade on SMA 20 cross", smaCross.strategyLabel)
        assertEquals("$150.00", smaCross.finalValueLabel)
        assertEquals("+$50.00", smaCross.profitLossLabel)
        assertEquals("+50.00%", smaCross.profitLossPercentLabel)
        assertEquals("1 buy · 2 sales", smaCross.tradesLabel)
        assertEquals("Out of the market", smaCross.positionLabel)
        assertEquals(PriceTrend.Up, smaCross.trend)
        assertEquals("Auto trading on SMA cross wins by $30.00", smaCross.verdict)
    }

    @Test
    fun testProviderMapsWinningBuyAndHoldStrategy() {
        val provider = StockChartUiProvider()
        val state = provider.initialState("AAPL", ChartTimeFrame.MONTH)

        val result = provider.reduce(
            state,
            StockChartMutation.ShowWhatIfResult(
                whatIfResult(
                    profitLoss = 40.0,
                    smaCross = SmaCrossResult(
                        symbol = "AAPL",
                        smaPeriod = 20,
                        invested = 100.0,
                        finalValue = 90.0,
                        profitLoss = -10.0,
                        profitLossPercent = -10.0,
                        buyCount = 1,
                        sellCount = 1,
                        holdingAtEnd = true,
                    ),
                ),
            ),
        )

        val smaCross = assertNotNull((result.whatIf as WhatIfState.Ready).model.smaCross)
        assertEquals(PriceTrend.Down, smaCross.trend)
        assertEquals("Still holding", smaCross.positionLabel)
        assertEquals("1 buy · 1 sale", smaCross.tradesLabel)
        assertEquals("Buy-and-hold wins by $50.00", smaCross.verdict)
    }

    @Test
    fun testProviderKeepsSmaCrossNullWhenNotRequested() {
        val provider = StockChartUiProvider()
        val state = provider.initialState("AAPL", ChartTimeFrame.MONTH)

        val result = provider.reduce(
            state,
            StockChartMutation.ShowWhatIfResult(whatIfResult(profitLoss = 20.0, smaCross = null)),
        )

        assertNull((result.whatIf as WhatIfState.Ready).model.smaCross)
    }

    private fun whatIfResult(profitLoss: Double, smaCross: SmaCrossResult?): WhatIfResult =
        WhatIfResult(
            symbol = "AAPL",
            quantity = 10.0,
            buyPrice = 10.0,
            buyTimestamp = 86_400L,
            currentPrice = 12.0,
            invested = 100.0,
            currentValue = 100.0 + profitLoss,
            profitLoss = profitLoss,
            profitLossPercent = profitLoss,
            smaCross = smaCross,
        )
}
