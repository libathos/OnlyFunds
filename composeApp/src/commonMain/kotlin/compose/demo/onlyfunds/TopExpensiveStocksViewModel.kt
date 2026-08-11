package compose.demo.onlyfunds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.onlyfunds.domain.usecases.GetQuoteUseCase
import io.onlyfunds.network.NetworkResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

sealed interface TopStocksAction {
    data object Load : TopStocksAction
    data object Retry : TopStocksAction
}

class TopExpensiveStocksViewModel(
    private val getQuote: GetQuoteUseCase = GetQuoteUseCase(),
    private val limit: Int = 10,
    private val refreshIntervalMillis: Long = 15_000L,
    private val uiProvider: TopStocksUiProvider = TopStocksUiProvider(
        refreshIntervalMillis = refreshIntervalMillis,
    ),
) : ViewModel() {

    private val _uiState = MutableStateFlow(uiProvider.initialState())
    val uiState: StateFlow<TopStocksUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null
    private var hasLoadedOnce = false

    fun onAction(action: TopStocksAction) {
        when (action) {
            TopStocksAction.Load -> startPolling()
            TopStocksAction.Retry -> {
                hasLoadedOnce = false
                restartPolling()
            }
        }
    }

    /**
     * Starts polling for quote updates. Safe to call whenever the screen
     * becomes visible: if a poll loop is already running this is a no-op, so
     * returning to the list resumes (rather than restarts) polling.
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch { pollLoop() }
    }

    /**
     * Stops polling. Called when the list screen leaves composition so no quote
     * requests fire while the user is on another screen (e.g. the chart).
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun restartPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch { pollLoop() }
    }

    private suspend fun pollLoop() {
        while (currentCoroutineContext().isActive) {
            if (!hasLoadedOnce) {
                emit(TopStocksMutation.ShowLoading)
                emit(fetchTopExpensiveStocks())
                hasLoadedOnce = true
            } else {
                emit(TopStocksMutation.StartRefresh)
                val startMark = TimeSource.Monotonic.markNow()
                val mutation = fetchTopExpensiveStocks()
                val remaining = REFRESH_INDICATOR_MIN_DURATION - startMark.elapsedNow()
                if (remaining.isPositive()) delay(remaining)
                if (mutation is TopStocksMutation.ShowStocks) {
                    emit(mutation)
                } else {
                    emit(TopStocksMutation.StopRefresh)
                }
            }
            delay(refreshIntervalMillis)
        }
    }

    private fun emit(mutation: TopStocksMutation) {
        _uiState.update { current -> uiProvider.reduce(current, mutation) }
    }

    private suspend fun fetchTopExpensiveStocks(): TopStocksMutation = coroutineScope {
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
            TopStocksMutation.ShowError(firstError?.message ?: "Failed to load quotes")
        } else {
            TopStocksMutation.ShowStocks(
                stocks.sortedByDescending { it.quote.current }.take(limit),
            )
        }
    }

    private companion object {
        val REFRESH_INDICATOR_MIN_DURATION = 600.milliseconds

        val CANDIDATE_SYMBOLS = listOf(
            "NVR", "BRK.A", "AZO", "BKNG", "SEB", "MELI", "AAPL", "MSFT",
            "GOOGL", "AMZN", "TSLA", "META", "NVDA", "NFLX", "ADBE", "AVGO",
        )
    }
}
