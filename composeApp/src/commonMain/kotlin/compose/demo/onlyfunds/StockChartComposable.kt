package compose.demo.onlyfunds

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.onlyfunds.domain.model.ChartTimeFrame

private val CHART_TIME_FRAMES: List<Pair<ChartTimeFrame, String>> = listOf(
    ChartTimeFrame.DAY to "1D",
    ChartTimeFrame.WEEK to "1W",
    ChartTimeFrame.MONTH to "1M",
    ChartTimeFrame.THREE_MONTHS to "3M",
    ChartTimeFrame.YEAR to "1Y",
    ChartTimeFrame.FIVE_YEARS to "5Y",
)

@Composable
fun StockChartScreen(
    symbol: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: StockChartViewModel = viewModel(key = symbol) { StockChartViewModel(symbol) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    StockChartContent(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
fun StockChartContent(
    uiState: StockChartUiState,
    onAction: (StockChartAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAlertDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        ChartHeader(symbol = uiState.symbol, onBack = onBack)

        Spacer(Modifier.height(16.dp))

        TimeFrameSelector(
            selected = uiState.selectedTimeFrame,
            onSelect = { onAction(StockChartAction.SelectTimeFrame(it)) },
        )

        Spacer(Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (val content = uiState.content) {
                is StockChartUiState.Content.Loading -> CenteredProgress()
                is StockChartUiState.Content.Error -> ChartError(
                    message = content.message,
                    onRetry = { onAction(StockChartAction.Retry) },
                )

                is StockChartUiState.Content.Chart -> ChartBody(content)
            }
        }

        Spacer(Modifier.height(16.dp))

        ChartActionButtons(
            onAlertClick = { showAlertDialog = true },
            onWhatIfClick = { /* What-if flow to be implemented */ },
        )
    }

    if (showAlertDialog) {
        PriceAlertDialog(
            symbol = uiState.symbol,
            onDismiss = { showAlertDialog = false },
            onConfirm = { targetPrice ->
                onAction(StockChartAction.SetAlert(targetPrice))
                showAlertDialog = false
            },
        )
    }
}

@Composable
private fun ChartActionButtons(
    onAlertClick: () -> Unit,
    onWhatIfClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onAlertClick, modifier = Modifier.weight(1f)) {
            Text("Alert")
        }
        OutlinedButton(onClick = onWhatIfClick, modifier = Modifier.weight(1f)) {
            Text("What-if")
        }
    }
}

@Composable
private fun PriceAlertDialog(
    symbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var priceText by remember { mutableStateOf("") }
    val targetPrice = priceText.trim().toDoubleOrNull()
    val isValid = targetPrice != null && targetPrice > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set price alert") },
        text = {
            Column {
                Text("Notify me when $symbol falls to or below:")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    singleLine = true,
                    prefix = { Text("$") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { targetPrice?.let(onConfirm) },
                enabled = isValid,
            ) {
                Text("Set alert")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ChartHeader(symbol: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "‹ Back",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onBack)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = symbol,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun ChartBody(content: StockChartUiState.Content.Chart) {
    Column(modifier = Modifier.fillMaxSize()) {
        PriceSummary(
            latestPrice = content.latestPrice,
            changeLabel = content.changeLabel,
            trend = content.trend,
        )

        Spacer(Modifier.height(16.dp))

        LineChart(
            points = content.points,
            trend = content.trend,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Low ${content.minLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = OnlyFundsTheme.colors.negative,
            )
            Text(
                text = "High ${content.maxLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = OnlyFundsTheme.colors.positive,
            )
        }
    }
}

@Composable
private fun PriceSummary(latestPrice: String, changeLabel: String, trend: PriceTrend) {
    val changeColor = when (trend) {
        PriceTrend.Up -> OnlyFundsTheme.colors.positive
        PriceTrend.Down -> OnlyFundsTheme.colors.negative
        PriceTrend.Flat -> MaterialTheme.colorScheme.onBackground
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = latestPrice,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = OnlyFundsTheme.colors.price,
        )
        Text(
            text = changeLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = changeColor,
        )
    }
}

@Composable
private fun LineChart(points: List<ChartPoint>, trend: PriceTrend, modifier: Modifier = Modifier) {
    val lineColor = when (trend) {
        PriceTrend.Up -> OnlyFundsTheme.colors.positive
        PriceTrend.Down -> OnlyFundsTheme.colors.negative
        PriceTrend.Flat -> OnlyFundsTheme.colors.price
    }
    Canvas(modifier = modifier) {
        val n = points.size
        if (n == 0) return@Canvas

        val width = size.width
        val height = size.height
        val prices = points.map { it.price }
        val min = prices.min()
        val max = prices.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0

        fun yOf(price: Double): Float {
            val norm = ((price - min) / range).toFloat().coerceIn(0f, 1f)
            val padded = 0.08f + norm * 0.84f
            return height - padded * height
        }

        val strokeWidth = 2.dp.toPx()

        if (n == 1) {
            val y = yOf(prices[0])
            drawLine(lineColor, Offset(0f, y), Offset(width, y), strokeWidth = strokeWidth)
            return@Canvas
        }

        val stepX = width / (n - 1)
        val line = Path()
        val area = Path()
        points.forEachIndexed { i, point ->
            val x = stepX * i
            val y = yOf(point.price)
            if (i == 0) {
                line.moveTo(x, y)
                area.moveTo(x, height)
                area.lineTo(x, y)
            } else {
                line.lineTo(x, y)
                area.lineTo(x, y)
            }
        }
        area.lineTo(width, height)
        area.close()

        drawPath(
            path = area,
            brush = Brush.verticalGradient(
                listOf(lineColor.copy(alpha = 0.35f), lineColor.copy(alpha = 0f)),
            ),
        )
        drawPath(
            path = line,
            color = lineColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
private fun TimeFrameSelector(selected: ChartTimeFrame, onSelect: (ChartTimeFrame) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CHART_TIME_FRAMES.forEach { (timeFrame, label) ->
            val isSelected = timeFrame == selected
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onBackground
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    )
                    .clickable { onSelect(timeFrame) }
                    .padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun CenteredProgress() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ChartError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
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

@Preview
@Composable
fun StockChartScreenPreview() {
    OnlyFundsTheme {
        StockChartContent(
            uiState = StockChartUiState(
                symbol = "AAPL",
                selectedTimeFrame = ChartTimeFrame.MONTH,
                content = StockChartUiState.Content.Chart(
                    points = listOf(
                        ChartPoint(1L, 180.0),
                        ChartPoint(2L, 184.5),
                        ChartPoint(3L, 182.2),
                        ChartPoint(4L, 190.1),
                        ChartPoint(5L, 195.7),
                        ChartPoint(6L, 193.3),
                        ChartPoint(7L, 201.0),
                    ),
                    latestPrice = "$201.00",
                    changeLabel = "+$21.00 (+11.67%)",
                    trend = PriceTrend.Up,
                    minLabel = "$180.00",
                    maxLabel = "$201.00",
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}
