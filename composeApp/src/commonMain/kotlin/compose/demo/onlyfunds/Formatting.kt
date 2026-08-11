package compose.demo.onlyfunds

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
