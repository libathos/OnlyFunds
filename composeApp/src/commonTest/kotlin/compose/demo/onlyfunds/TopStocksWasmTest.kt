package compose.demo.onlyfunds

import compose.demo.onlyfunds.topStocksScreen.mvi.TopExpensiveStocksViewModel
import compose.demo.onlyfunds.topStocksScreen.mvi.TopStocksUiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TopStocksWasmTest {

    @Test
    fun testViewModelPolling() = runTest {
        val viewModel = TopExpensiveStocksViewModel()
        viewModel.startPolling()
        val state = viewModel.uiState.first { it.content !is TopStocksUiState.Content.Loading }
        println("ViewModel state after polling: $state")
        viewModel.stopPolling()
    }
}
