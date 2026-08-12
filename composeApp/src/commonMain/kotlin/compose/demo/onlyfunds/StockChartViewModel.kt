package compose.demo.onlyfunds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.onlyfunds.domain.model.AlertDirection
import io.onlyfunds.domain.model.ChartTimeFrame
import io.onlyfunds.domain.model.PriceAlert
import io.onlyfunds.domain.usecases.CalculateWhatIfUseCase
import io.onlyfunds.domain.usecases.GetStockCandlesUseCase
import io.onlyfunds.network.NetworkResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

sealed interface StockChartAction {
    data object Retry : StockChartAction
    data class SelectTimeFrame(val timeFrame: ChartTimeFrame) : StockChartAction
    data class SetAlert(val targetPrice: Double) : StockChartAction
    data class CalculateWhatIf(
        val buyPrice: Double,
        val quantity: Double,
        val buyTimestamp: Long,
    ) : StockChartAction
    data object DismissWhatIf : StockChartAction
}

class StockChartViewModel(
    private val symbol: String,
    private val getStockCandles: GetStockCandlesUseCase = GetStockCandlesUseCase(),
    private val calculateWhatIf: CalculateWhatIfUseCase = CalculateWhatIfUseCase(),
    private val uiProvider: StockChartUiProvider = StockChartUiProvider(),
    initialTimeFrame: ChartTimeFrame = ChartTimeFrame.MONTH,
) : ViewModel() {

    private val _uiState = MutableStateFlow(uiProvider.initialState(symbol, initialTimeFrame))
    val uiState: StateFlow<StockChartUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadCandles(initialTimeFrame)
    }

    fun onAction(action: StockChartAction) {
        when (action) {
            StockChartAction.Retry -> loadCandles(_uiState.value.selectedTimeFrame)
            is StockChartAction.SelectTimeFrame -> {
                if (action.timeFrame == _uiState.value.selectedTimeFrame) return
                emit(StockChartMutation.SelectTimeFrame(action.timeFrame))
                loadCandles(action.timeFrame)
            }

            is StockChartAction.SetAlert -> PriceAlertStore.setAlert(
                PriceAlert(
                    symbol = symbol,
                    targetPrice = action.targetPrice,
                    direction = AlertDirection.BELOW,
                ),
            )

            is StockChartAction.CalculateWhatIf -> runWhatIf(action)

            StockChartAction.DismissWhatIf -> emit(StockChartMutation.DismissWhatIf)
        }
    }

    private fun runWhatIf(action: StockChartAction.CalculateWhatIf) {
        viewModelScope.launch {
            emit(StockChartMutation.ShowWhatIfLoading)
            val mutation = when (
                val response = calculateWhatIf(
                    symbol = symbol,
                    buyPrice = action.buyPrice,
                    quantity = action.quantity,
                    buyTimestamp = action.buyTimestamp,
                )
            ) {
                is NetworkResponse.Success -> StockChartMutation.ShowWhatIfResult(response.data)
                is NetworkResponse.Error -> StockChartMutation.ShowWhatIfError(response.message)
            }
            emit(mutation)
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadCandles(timeFrame: ChartTimeFrame) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            emit(StockChartMutation.ShowLoading)
            val nowEpochSeconds = Clock.System.now().epochSeconds
            val mutation = when (val response = getStockCandles(symbol, timeFrame, nowEpochSeconds)) {
                is NetworkResponse.Success -> StockChartMutation.ShowChart(response.data)
                is NetworkResponse.Error -> StockChartMutation.ShowError(response.message)
            }
            emit(mutation)
        }
    }

    private fun emit(mutation: StockChartMutation) {
        _uiState.update { current -> uiProvider.reduce(current, mutation) }
    }
}
