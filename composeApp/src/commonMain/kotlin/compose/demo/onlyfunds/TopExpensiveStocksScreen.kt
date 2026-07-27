package compose.demo.onlyfunds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.onlyfunds.domain.usecases.GetQuoteUseCase
import io.onlyfunds.network.NetworkResponse
import io.onlyfunds.network.Quote
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal val BackgroundColor = Color(0xFF2B3A4A)
private val OnBackgroundColor = Color(0xFFECEFF4)
private val AccentColor = Color(0xFF7FB2E5)
private val PriceColor = Color(0xFF8FD9A8)
private val DividerColor = Color(0x33FFFFFF)

private val CANDIDATE_SYMBOLS = listOf(
    "NVR", "BRK.A", "AZO", "BKNG", "SEB", "MELI", "AAPL", "MSFT",
    "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "ADBE", "AVGO",
)

data class StockQuote(val symbol: String, val quote: Quote)

sealed interface TopStocksUiState {
    data object Loading : TopStocksUiState
    data class Success(val stocks: List<StockQuote>) : TopStocksUiState
    data class Error(val message: String) : TopStocksUiState
}

private suspend fun loadTopExpensiveStocks(
    getQuote: GetQuoteUseCase,
    limit: Int = 10,
): TopStocksUiState = coroutineScope {
    val results = CANDIDATE_SYMBOLS
        .map { symbol -> async { symbol to getQuote(symbol) } }
        .awaitAll()

    val stocks = results.mapNotNull { (symbol, response) ->
        when (response) {
            is NetworkResponse.Success -> StockQuote(symbol, response.data)
            is NetworkResponse.Error -> null
        }
    }

    if (stocks.isEmpty()) {
        val firstError = results
            .map { it.second }
            .filterIsInstance<NetworkResponse.Error>()
            .firstOrNull()
        TopStocksUiState.Error(firstError?.message ?: "Failed to load quotes")
    } else {
        TopStocksUiState.Success(
            stocks.sortedByDescending { it.quote.current }.take(limit)
        )
    }
}

@Composable
fun TopExpensiveStocksScreen(
    modifier: Modifier = Modifier,
    getQuote: GetQuoteUseCase = GetQuoteUseCase(),
) {
    val uiState by produceState<TopStocksUiState>(TopStocksUiState.Loading) {
        value = loadTopExpensiveStocks(getQuote)
    }

    TopExpensiveStocksContent(uiState = uiState, modifier = modifier)
}

@Composable
fun TopExpensiveStocksContent(
    uiState: TopStocksUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp),
    ) {
        Text(
            text = "Top 10 Most Expensive Stocks",
            style = MaterialTheme.typography.headlineSmall,
            color = OnBackgroundColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        when (uiState) {
            is TopStocksUiState.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = AccentColor)
                }
            }

            is TopStocksUiState.Error -> {
                Text(
                    text = uiState.message,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            is TopStocksUiState.Success -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(uiState.stocks) { index, stock ->
                        StockRow(rank = index + 1, stock = stock)
                        Divider(color = DividerColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockRow(rank: Int, stock: StockQuote) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$rank",
            style = MaterialTheme.typography.titleMedium,
            color = AccentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 16.dp),
        )
        Text(
            text = stock.symbol,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnBackgroundColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "$${formatPrice(stock.quote.current)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = PriceColor,
        )
    }
}

private fun formatPrice(value: Double): String {
    val rounded = (value * 100).toLong()
    val whole = rounded / 100
    val cents = (rounded % 100).let { if (it < 0) -it else it }
    val centsText = cents.toString().padStart(2, '0')
    return "$whole.$centsText"
}

private fun sampleQuote(price: Double) = Quote(
    current = price,
    change = 1.23,
    percentChange = 0.45,
    high = price + 5,
    low = price - 5,
    open = price - 2,
    previousClose = price - 1,
    timestamp = 1_582_641_000,
)

@Preview
@Composable
fun TopExpensiveStocksScreenPreview() {
    MaterialTheme {
        TopExpensiveStocksContent(
            uiState = TopStocksUiState.Success(
                stocks = listOf(
                    StockQuote("SEB", sampleQuote(4820.11)),
                    StockQuote("NVR", sampleQuote(7345.50)),
                    StockQuote("BKNG", sampleQuote(3980.75)),
                    StockQuote("AZO", sampleQuote(2960.00)),
                    StockQuote("MELI", sampleQuote(1620.40)),
                    StockQuote("BRK.A", sampleQuote(628000.00)),
                    StockQuote("ADBE", sampleQuote(540.30)),
                    StockQuote("AVGO", sampleQuote(1320.90)),
                    StockQuote("NFLX", sampleQuote(690.15)),
                    StockQuote("AAPL", sampleQuote(212.05)),
                ).sortedByDescending { it.quote.current },
            ),
        )
    }
}

