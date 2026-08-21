package io.onlyfunds.network

import io.ktor.client.statement.request
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UrlTest {

    @Test
    fun testUrlGeneration() = runTest {
        val quoteService = QuoteService()
        val res = quoteService.getQuote("AAPL")
        println("Result: $res")
    }
}
