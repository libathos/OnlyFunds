package compose.demo.onlyfunds

import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun TopExpensiveStocksScreen(
    modifier: Modifier = Modifier,
    viewModel: TopExpensiveStocksViewModel = viewModel { TopExpensiveStocksViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TopExpensiveStocksContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun TopExpensiveStocksContent(
    uiState: TopStocksUiState,
    onAction: (TopStocksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        Header(title = uiState.title, isRefreshing = uiState.isRefreshing)

        when (val content = uiState.content) {
            is TopStocksUiState.Content.Loading -> LoadingContent()
            is TopStocksUiState.Content.Error -> ErrorContent(
                message = content.message,
                onRetry = { onAction(TopStocksAction.Retry) },
            )
            is TopStocksUiState.Content.Stocks -> StocksContent(rows = content.rows)
        }
    }
}

@Composable
private fun Header(title: String, isRefreshing: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(
            visible = isRefreshing,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Updating…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = "Tap to retry",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 12.dp)
                .clickable(onClick = onRetry),
        )
    }
}

@Composable
private fun StocksContent(rows: List<StockRowUiModel>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(rows, key = { it.symbol }) { row ->
            Column(modifier = Modifier.animateItem()) {
                StockRow(row)
                Divider(color = OnlyFundsTheme.colors.divider)
            }
        }
    }
}

@Composable
private fun StockRow(model: StockRowUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = model.rank,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 16.dp),
        )
        Text(
            text = model.symbol,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        AnimatedPrice(model = model)
    }
}

@Composable
private fun AnimatedPrice(model: StockRowUiModel) {
    val baseColor = OnlyFundsTheme.colors.price
    val upColor = OnlyFundsTheme.colors.positive
    val downColor = OnlyFundsTheme.colors.negative
    val flashColor = remember { Animatable(baseColor) }

    LaunchedEffect(model.priceValue) {
        val target = when (model.trend) {
            PriceTrend.Up -> upColor
            PriceTrend.Down -> downColor
            PriceTrend.Flat -> null
        }
        if (target != null) {
            flashColor.snapTo(target)
            flashColor.animateTo(baseColor, animationSpec = tween(durationMillis = 900))
        }
    }

    AnimatedContent(
        targetState = model.price,
        transitionSpec = {
            if (model.trend == PriceTrend.Down) {
                (slideInVertically { height -> -height } + fadeIn()) togetherWith
                    (slideOutVertically { height -> height } + fadeOut())
            } else {
                (slideInVertically { height -> height } + fadeIn()) togetherWith
                    (slideOutVertically { height -> -height } + fadeOut())
            }
        },
        label = "price",
    ) { price ->
        Text(
            text = price,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = flashColor.value,
        )
    }
}

@Preview
@Composable
fun TopExpensiveStocksScreenPreview() {
    OnlyFundsTheme {
        TopExpensiveStocksContent(
            uiState = TopStocksUiState(
                title = "Top 10 Most Expensive Stocks",
                content = TopStocksUiState.Content.Stocks(
                    rows = listOf(
                        StockRowUiModel("1", "BRK.A", "$628000.00", 628000.00, PriceTrend.Flat),
                        StockRowUiModel("2", "NVR", "$7345.50", 7345.50, PriceTrend.Up),
                        StockRowUiModel("3", "SEB", "$4820.11", 4820.11, PriceTrend.Down),
                        StockRowUiModel("4", "BKNG", "$3980.75", 3980.75, PriceTrend.Up),
                        StockRowUiModel("5", "AZO", "$2960.00", 2960.00, PriceTrend.Flat),
                        StockRowUiModel("6", "MELI", "$1620.40", 1620.40, PriceTrend.Down),
                        StockRowUiModel("7", "AVGO", "$1320.90", 1320.90, PriceTrend.Up),
                        StockRowUiModel("8", "NFLX", "$690.15", 690.15, PriceTrend.Flat),
                        StockRowUiModel("9", "ADBE", "$540.30", 540.30, PriceTrend.Down),
                        StockRowUiModel("10", "AAPL", "$212.05", 212.05, PriceTrend.Up),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}
