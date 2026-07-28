package io.onlyfunds.domain.model

enum class CandleResolution(val apiValue: String) {
    ONE_MINUTE("1"),
    FIVE_MINUTES("5"),
    FIFTEEN_MINUTES("15"),
    THIRTY_MINUTES("30"),
    ONE_HOUR("60"),
    DAY("D"),
    WEEK("W"),
    MONTH("M"),
}
