package io.onlyfunds.domain.mapper

import io.onlyfunds.domain.model.Quote
import io.onlyfunds.network.Quote as NetworkQuote

fun NetworkQuote.toDomain(): Quote = Quote(
    current = current,
    change = change,
    percentChange = percentChange,
    high = high,
    low = low,
    open = open,
    previousClose = previousClose,
    timestamp = timestamp,
)
