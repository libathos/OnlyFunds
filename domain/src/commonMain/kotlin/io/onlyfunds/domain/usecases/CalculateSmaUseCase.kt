package io.onlyfunds.domain.usecases

/**
 * Simple moving average of a series of closing prices, used both by the chart
 * overlay and by the SMA cross backtest so the two always agree.
 */
class CalculateSmaUseCase {

    /** Average per index, `null` while there is not enough history for a full window. */
    operator fun invoke(closes: List<Double>, period: Int = DEFAULT_PERIOD): List<Double?> {
        val effective = effectivePeriod(period, closes.size)
        var sum = 0.0
        return closes.mapIndexed { index, close ->
            sum += close
            if (index >= effective) sum -= closes[index - effective]
            if (index >= effective - 1) sum / effective else null
        }
    }

    /**
     * Shrinks the requested period when the series is too short to produce
     * averages with it, so short time frames still yield a usable line.
     */
    fun effectivePeriod(requested: Int, size: Int): Int {
        val maxPeriod = (size / 2).coerceAtLeast(MIN_PERIOD)
        return requested.coerceIn(MIN_PERIOD, maxPeriod)
    }

    companion object {
        const val DEFAULT_PERIOD: Int = 20
        private const val MIN_PERIOD = 2
    }
}
