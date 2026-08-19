package io.onlyfunds.domain.usecases

import io.onlyfunds.domain.model.Candle
import io.onlyfunds.domain.model.SmaCrossResult

/**
 * Backtests the classic moving-average crossover strategy on the closes of the
 * chart the user is looking at: every close above the simple moving average is
 * treated as a buy, every close below it as a sale of the whole position.
 *
 * The strategy starts with the same amount of money the buy-and-hold scenario
 * invests, so both results are directly comparable.
 */
class BacktestSmaCrossUseCase(
    private val calculateSma: CalculateSmaUseCase = CalculateSmaUseCase(),
) {

    operator fun invoke(
        symbol: String,
        candles: List<Candle>,
        buyTimestamp: Long,
        buyPrice: Double,
        quantity: Double,
        currentPrice: Double,
        smaPeriod: Int = DEFAULT_SMA_PERIOD,
    ): SmaCrossResult? {
        val closes = candles.map { it.close }
        val invested = buyPrice * quantity
        if (closes.size < MIN_CANDLES || invested <= 0.0 || currentPrice <= 0.0) return null

        val startIndex = candles.indexOfFirst { it.timestamp >= buyTimestamp }
        if (startIndex < 0) return null

        val period = calculateSma.effectivePeriod(smaPeriod, closes.size)
        val averages = calculateSma(closes, period)

        var cash = invested
        var shares = 0.0
        var buyCount = 0
        var sellCount = 0

        for (index in startIndex..closes.lastIndex) {
            val average = averages[index] ?: continue
            val close = closes[index]
            if (close <= 0.0) continue
            when {
                close > average && shares == 0.0 -> {
                    shares = cash / close
                    cash = 0.0
                    buyCount++
                }

                close < average && shares > 0.0 -> {
                    cash = shares * close
                    shares = 0.0
                    sellCount++
                }
            }
        }

        val holdingAtEnd = shares > 0.0
        val finalValue = if (holdingAtEnd) shares * currentPrice else cash
        val profitLoss = finalValue - invested

        return SmaCrossResult(
            symbol = symbol,
            smaPeriod = period,
            invested = invested,
            finalValue = finalValue,
            profitLoss = profitLoss,
            profitLossPercent = profitLoss / invested * 100.0,
            buyCount = buyCount,
            sellCount = sellCount,
            holdingAtEnd = holdingAtEnd,
        )
    }

    companion object {
        const val DEFAULT_SMA_PERIOD: Int = CalculateSmaUseCase.DEFAULT_PERIOD
        private const val MIN_CANDLES = 4
    }
}
