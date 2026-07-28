package io.onlyfunds.domain.model

data class CandleRange(val from: Long, val to: Long)

enum class ChartTimeFrame(
    val durationSeconds: Long,
    val resolution: CandleResolution,
) {
    DAY(24L * 60 * 60, CandleResolution.FIVE_MINUTES),
    WEEK(7L * 24 * 60 * 60, CandleResolution.THIRTY_MINUTES),
    MONTH(30L * 24 * 60 * 60, CandleResolution.DAY),
    THREE_MONTHS(90L * 24 * 60 * 60, CandleResolution.DAY),
    YEAR(365L * 24 * 60 * 60, CandleResolution.DAY),
    FIVE_YEARS(5L * 365 * 24 * 60 * 60, CandleResolution.WEEK);

    fun toRange(nowEpochSeconds: Long): CandleRange =
        CandleRange(from = nowEpochSeconds - durationSeconds, to = nowEpochSeconds)
}
