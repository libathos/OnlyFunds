package compose.demo.onlyfunds.application.misc

import kotlin.math.abs

fun formatPrice(value: Double): String {
    val rounded = (value * 100).toLong()
    val whole = rounded / 100
    val cents = abs(rounded % 100)
    val centsText = cents.toString().padStart(2, '0')
    return "$whole.$centsText"
}

fun formatUsd(value: Double): String = "$" + formatPrice(value)

fun formatSignedUsd(value: Double): String {
    val sign = if (value < 0) "-" else "+"
    return "$sign$" + formatPrice(abs(value))
}

fun formatSignedPercent(value: Double): String {
    val sign = if (value < 0) "-" else "+"
    val rounded = (abs(value) * 100).toLong()
    val whole = rounded / 100
    val frac = (rounded % 100).toString().padStart(2, '0')
    return "$sign$whole.$frac%"
}

fun formatQuantity(value: Double): String {
    val rounded = (value * 100).toLong()
    return if (rounded % 100 == 0L) {
        (rounded / 100).toString()
    } else {
        formatPrice(value)
    }
}

/**
 * Formats an epoch-seconds timestamp as a UTC "DD/MM/YYYY" date, using the
 * civil-from-days algorithm so no platform date library is required.
 */
fun formatDate(epochSeconds: Long): String {
    val days = epochSeconds.floorDiv(86_400L)
    val z = days + 719_468L
    val era = (if (z >= 0) z else z - 146_096L) / 146_097L
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
    val year = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val day = doy - (153L * mp + 2L) / 5L + 1L
    val month = if (mp < 10L) mp + 3L else mp - 9L
    val calendarYear = if (month <= 2L) year + 1L else year
    val mm = month.toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "$dd/$mm/$calendarYear"
}
