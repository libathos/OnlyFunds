package compose.demo.onlyfunds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    OnlyFundsTheme {
        val screenModifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeContentPadding()

        var selectedSymbol by rememberSaveable { mutableStateOf<String?>(null) }
        val symbol = selectedSymbol

        if (symbol == null) {
            TopExpensiveStocksScreen(
                modifier = screenModifier,
                onStockSelected = { selectedSymbol = it },
            )
        } else {
            StockChartScreen(
                symbol = symbol,
                modifier = screenModifier,
                onBack = { selectedSymbol = null },
            )
        }
    }
}
