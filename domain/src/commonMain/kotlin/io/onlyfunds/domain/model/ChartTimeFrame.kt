package io.onlyfunds.domain.model

data class CandleRange(val from: Long, val to: Long)

enum class ChartTimeFrame(
    val durationSeconds: Long,
    val resolution: CandleResolution,
    val yahooRange: String,
    val yahooInterval: String,
) {
    DAY(24L * 60 * 60, CandleResolution.FIVE_MINUTES, "1d", "5m"),
    WEEK(7L * 24 * 60 * 60, CandleResolution.THIRTY_MINUTES, "5d", "30m"),
    MONTH(30L * 24 * 60 * 60, CandleResolution.DAY, "1mo", "1d"),
    THREE_MONTHS(90L * 24 * 60 * 60, CandleResolution.DAY, "3mo", "1d"),
    YEAR(365L * 24 * 60 * 60, CandleResolution.DAY, "1y", "1d"),
    FIVE_YEARS(5L * 365 * 24 * 60 * 60, CandleResolution.WEEK, "5y", "1wk");

    fun toRange(nowEpochSeconds: Long): CandleRange =
        CandleRange(from = nowEpochSeconds - durationSeconds, to = nowEpochSeconds)
}
