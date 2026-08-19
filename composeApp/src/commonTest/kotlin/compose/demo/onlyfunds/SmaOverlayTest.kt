package compose.demo.onlyfunds

import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartMutation
import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartUiProvider
import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartUiState
import io.onlyfunds.domain.model.Candle
import io.onlyfunds.domain.model.ChartTimeFrame
import io.onlyfunds.domain.model.StockCandles
import io.onlyfunds.domain.usecases.CalculateSmaUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmaOverlayTest {

    private val calculateSma = CalculateSmaUseCase()

    private fun candlesOf(closes: List<Double>): StockCandles =
        StockCandles(
            symbol = "AAPL",
            candles = closes.mapIndexed { index, close ->
                Candle(
                    timestamp = (index + 1) * 86_400L,
                    open = close,
                    high = close,
                    low = close,
                    close = close,
                    volume = 0.0,
                )
            },
            hasData = closes.isNotEmpty(),
        )

    private fun chartContentOf(closes: List<Double>): StockChartUiState.Content {
        val provider = StockChartUiProvider()
        val state = provider.initialState("AAPL", ChartTimeFrame.MONTH)
        return provider.reduce(state, StockChartMutation.ShowChart(candlesOf(closes))).content
    }

    @Test
    fun testAverageIsNullUntilTheWindowIsFull() {
        val closes = listOf(10.0, 20.0, 30.0, 40.0, 50.0, 60.0)
        val averages = calculateSma(closes, period = 3)

        assertEquals(closes.size, averages.size)
        assertNull(averages[0])
        assertNull(averages[1])
        assertEquals(20.0, assertNotNull(averages[2]), 1e-9)
        assertEquals(30.0, assertNotNull(averages[3]), 1e-9)
    }

    @Test
    fun testPeriodShrinksForShortSeries() {
        assertEquals(3, calculateSma.effectivePeriod(CalculateSmaUseCase.DEFAULT_PERIOD, size = 6))
        assertEquals(20, calculateSma.effectivePeriod(CalculateSmaUseCase.DEFAULT_PERIOD, size = 60))
        assertEquals(2, calculateSma.effectivePeriod(CalculateSmaUseCase.DEFAULT_PERIOD, size = 2))
    }

    @Test
    fun testChartContentCarriesTheSmaOverlay() {
        val closes = List(60) { 100.0 + it }
        val chart = chartContentOf(closes) as StockChartUiState.Content.Chart

        assertEquals("SMA 20", chart.smaLabel)
        assertEquals(closes.size, chart.smaValues.size)
        assertTrue(chart.smaValues.take(19).all { it == null })
        assertEquals(
            closes.take(20).average(),
            assertNotNull(chart.smaValues[19]),
            1e-9,
        )
    }

    @Test
    fun testShortChartStillGetsAnOverlay() {
        val chart = chartContentOf(listOf(10.0, 12.0, 14.0, 16.0, 18.0, 20.0))
            as StockChartUiState.Content.Chart

        assertEquals("SMA 3", chart.smaLabel)
        assertEquals(12.0, assertNotNull(chart.smaValues[2]), 1e-9)
    }

    @Test
    fun testSingleCandleHasNoOverlay() {
        val chart = chartContentOf(listOf(10.0)) as StockChartUiState.Content.Chart

        assertTrue(chart.smaValues.isEmpty())
    }
}
