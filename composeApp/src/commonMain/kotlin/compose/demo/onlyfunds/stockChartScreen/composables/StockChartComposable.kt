package compose.demo.onlyfunds.stockChartScreen.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import compose.demo.onlyfunds.theme.OnlyFundsTheme
import compose.demo.onlyfunds.topStocksScreen.mvi.PriceTrend
import compose.demo.onlyfunds.application.misc.LockScreenOrientation
import compose.demo.onlyfunds.application.misc.ScreenOrientation
import compose.demo.onlyfunds.application.misc.formatDate
import compose.demo.onlyfunds.application.misc.formatUsd
import compose.demo.onlyfunds.stockChartScreen.mvi.ChartPoint
import compose.demo.onlyfunds.stockChartScreen.mvi.SmaCrossUiModel
import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartAction
import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartUiState
import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartViewModel
import compose.demo.onlyfunds.stockChartScreen.mvi.WhatIfState
import compose.demo.onlyfunds.stockChartScreen.mvi.WhatIfUiModel
import io.onlyfunds.domain.model.ChartTimeFrame
import kotlin.math.roundToInt

internal val CHART_TIME_FRAMES: List<Pair<ChartTimeFrame, String>> = listOf(
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
    viewModel: StockChartViewModel = viewModel(key = symbol) {
        StockChartViewModel(
            symbol
        )
    },
) {
    LockScreenOrientation(ScreenOrientation.PORTRAIT)

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
    var showMaximizedChart by remember { mutableStateOf(false) }
    var whatIfMode by remember { mutableStateOf(false) }
    var buyPoint by remember { mutableStateOf<ChartPoint?>(null) }
    var askQuantity by remember { mutableStateOf(false) }

    val chart = uiState.content as? StockChartUiState.Content.Chart

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
    ) {
        ChartHeader(
            symbol = uiState.symbol,
            onBack = onBack,
            maximizeEnabled = chart != null,
            onMaximize = { showMaximizedChart = true },
        )

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

                is StockChartUiState.Content.Chart -> ChartBody(
                    content = content,
                    selectedPoint = buyPoint,
                    selectionEnabled = whatIfMode,
                    onPointSelected = { point ->
                        buyPoint = point
                        whatIfMode = false
                        askQuantity = true
                    },
                )
            }
        }

        if (whatIfMode) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap a point on the chart to pick your buy date.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(16.dp))

        ChartActionButtons(
            whatIfActive = whatIfMode,
            whatIfEnabled = chart != null,
            onAlertClick = { showAlertDialog = true },
            onWhatIfClick = { whatIfMode = !whatIfMode },
        )
    }

    if (showMaximizedChart && chart != null) {
        MaximizedStockChartDialog(
            symbol = uiState.symbol,
            content = chart,
            selectedTimeFrame = uiState.selectedTimeFrame,
            onSelectTimeFrame = { onAction(StockChartAction.SelectTimeFrame(it)) },
            onDismiss = { showMaximizedChart = false },
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

    val point = buyPoint
    if (askQuantity && point != null) {
        WhatIfQuantityDialog(
            symbol = uiState.symbol,
            point = point,
            onDismiss = {
                askQuantity = false
                buyPoint = null
            },
            onConfirm = { quantity, autoTradeOnSmaCross ->
                askQuantity = false
                onAction(
                    StockChartAction.CalculateWhatIf(
                        buyPrice = point.price,
                        quantity = quantity,
                        buyTimestamp = point.timestamp,
                        autoTradeOnSmaCross = autoTradeOnSmaCross,
                    ),
                )
            },
        )
    }

    uiState.whatIf?.let { whatIf ->
        WhatIfResultDialog(
            state = whatIf,
            onDismiss = {
                onAction(StockChartAction.DismissWhatIf)
                buyPoint = null
            },
        )
    }
}

@Composable
private fun ChartActionButtons(
    whatIfActive: Boolean,
    whatIfEnabled: Boolean,
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
        if (whatIfActive) {
            Button(
                onClick = onWhatIfClick,
                enabled = whatIfEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel What-if")
            }
        } else {
            OutlinedButton(
                onClick = onWhatIfClick,
                enabled = whatIfEnabled,
                modifier = Modifier.weight(1f),
            ) {
                Text("What-if")
            }
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
private fun WhatIfQuantityDialog(
    symbol: String,
    point: ChartPoint,
    onDismiss: () -> Unit,
    onConfirm: (Double, Boolean) -> Unit,
) {
    var quantityText by remember { mutableStateOf("") }
    var autoTradeOnSmaCross by remember { mutableStateOf(false) }
    val quantity = quantityText.trim().toDoubleOrNull()
    val isValid = quantity != null && quantity > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What if…") },
        text = {
            Column {
                Text(
                    "Buy $symbol on ${formatDate(point.timestamp)} " +
                        "at ${formatUsd(point.price)}.",
                )
                Spacer(Modifier.height(12.dp))
                Text("How many shares would you have bought?")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    singleLine = true,
                    placeholder = { Text("0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                Spacer(Modifier.height(12.dp))
                AutoTradeToggle(
                    checked = autoTradeOnSmaCross,
                    onCheckedChange = { autoTradeOnSmaCross = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { quantity?.let { onConfirm(it, autoTradeOnSmaCross) } },
                enabled = isValid,
            ) {
                Text("Calculate")
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
private fun AutoTradeToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Auto trade on SMA Cross",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Buy when a close is above the moving average, " +
                    "sell when it closes below it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun WhatIfResultDialog(state: WhatIfState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What-if result") },
        text = {
            when (state) {
                WhatIfState.Calculating -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Fetching today's price…")
                }

                is WhatIfState.Error -> Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                )

                is WhatIfState.Ready -> WhatIfDetails(state.model)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        },
    )
}

@Composable
private fun WhatIfDetails(model: WhatIfUiModel) {
    val profitColor = when (model.trend) {
        PriceTrend.Up -> OnlyFundsTheme.colors.positive
        PriceTrend.Down -> OnlyFundsTheme.colors.negative
        PriceTrend.Flat -> MaterialTheme.colorScheme.onSurface
    }
    val headline = when (model.trend) {
        PriceTrend.Up -> "You would have made"
        PriceTrend.Down -> "You would have lost"
        PriceTrend.Flat -> "You would have broken even"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        WhatIfRow("Shares", model.quantityLabel)
        WhatIfRow("Buy date", model.buyDateLabel)
        WhatIfRow("Buy price", model.buyPriceLabel)
        WhatIfRow("Today's price", model.currentPriceLabel)
        WhatIfRow("Invested", model.investedLabel)
        WhatIfRow("Value today", model.currentValueLabel)

        Spacer(Modifier.height(12.dp))

        Text(
            text = headline,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${model.profitLossLabel} (${model.profitLossPercentLabel})",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = profitColor,
        )

        model.smaCross?.let { smaCross ->
            Spacer(Modifier.height(16.dp))
            StrategyComparison(
                smaCross = smaCross,
                holdProfitLossLabel = model.profitLossLabel,
                holdProfitLossPercentLabel = model.profitLossPercentLabel,
                holdValueLabel = model.currentValueLabel,
                holdTrend = model.trend,
            )
        }
    }
}

@Composable
private fun StrategyComparison(
    smaCross: SmaCrossUiModel,
    holdProfitLossLabel: String,
    holdProfitLossPercentLabel: String,
    holdValueLabel: String,
    holdTrend: PriceTrend,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Strategy comparison",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(8.dp))

        StrategyCard(
            title = smaCross.strategyLabel,
            valueLabel = smaCross.finalValueLabel,
            profitLossLabel = smaCross.profitLossLabel,
            profitLossPercentLabel = smaCross.profitLossPercentLabel,
            footnote = "${smaCross.tradesLabel} · ${smaCross.positionLabel}",
            trend = smaCross.trend,
        )

        Spacer(Modifier.height(8.dp))

        StrategyCard(
            title = "Buy and hold",
            valueLabel = holdValueLabel,
            profitLossLabel = holdProfitLossLabel,
            profitLossPercentLabel = holdProfitLossPercentLabel,
            footnote = "Bought once, never sold",
            trend = holdTrend,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = smaCross.verdict,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun StrategyCard(
    title: String,
    valueLabel: String,
    profitLossLabel: String,
    profitLossPercentLabel: String,
    footnote: String,
    trend: PriceTrend,
) {
    val trendColor = when (trend) {
        PriceTrend.Up -> OnlyFundsTheme.colors.positive
        PriceTrend.Down -> OnlyFundsTheme.colors.negative
        PriceTrend.Flat -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(trendColor.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = profitLossLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = trendColor,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = profitLossPercentLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = trendColor,
            )
        }
        Text(
            text = "Value today $valueLabel",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = footnote,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WhatIfRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ChartHeader(
    symbol: String,
    onBack: () -> Unit,
    maximizeEnabled: Boolean,
    onMaximize: () -> Unit,
) {
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
        Spacer(Modifier.weight(1f))
        if (maximizeEnabled) {
            MaximizeChartButton(onClick = onMaximize)
        }
    }
}

@Composable
private fun MaximizeChartButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        MaximizeGlyph(
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ChartBody(
    content: StockChartUiState.Content.Chart,
    selectedPoint: ChartPoint?,
    selectionEnabled: Boolean,
    onPointSelected: (ChartPoint) -> Unit,
) {
    var showSma by remember { mutableStateOf(true) }

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
            selectedPoint = selectedPoint,
            selectionEnabled = selectionEnabled,
            onPointSelected = onPointSelected,
            modifier = Modifier.fillMaxWidth().weight(1f),
            smaValues = if (showSma) content.smaValues else emptyList(),
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Low ${content.minLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = OnlyFundsTheme.colors.negative,
            )
            if (content.smaValues.isNotEmpty()) {
                SmaLegendToggle(
                    label = content.smaLabel,
                    checked = showSma,
                    onCheckedChange = { showSma = it },
                )
            }
            Text(
                text = "High ${content.maxLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = OnlyFundsTheme.colors.positive,
            )
        }
    }
}

@Composable
internal fun SmaLegendToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (checked) {
        OnlyFundsTheme.colors.sma
    } else {
        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DashedLineGlyph(
            color = color,
            modifier = Modifier.width(18.dp).height(8.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}

@Composable
private fun DashedLineGlyph(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 1.5.dp.toPx()
        val y = size.height / 2f
        val dash = size.width / 3f
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(dash, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = Offset(dash * 2f, y),
            end = Offset(size.width, y),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun PriceSummary(latestPrice: String, changeLabel: String, trend: PriceTrend) {
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
internal fun LineChart(
    points: List<ChartPoint>,
    trend: PriceTrend,
    selectedPoint: ChartPoint?,
    selectionEnabled: Boolean,
    onPointSelected: (ChartPoint) -> Unit,
    modifier: Modifier = Modifier,
    smaValues: List<Double?> = emptyList(),
) {
    val lineColor = when (trend) {
        PriceTrend.Up -> OnlyFundsTheme.colors.positive
        PriceTrend.Down -> OnlyFundsTheme.colors.negative
        PriceTrend.Flat -> OnlyFundsTheme.colors.price
    }
    val markerFill = MaterialTheme.colorScheme.background
    val guideColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
    val smaColor = OnlyFundsTheme.colors.sma

    val tapModifier = if (selectionEnabled && points.isNotEmpty()) {
        Modifier.pointerInput(points) {
            detectTapGestures { offset ->
                val n = points.size
                val index = if (n == 1) {
                    0
                } else {
                    val stepX = size.width.toFloat() / (n - 1)
                    (offset.x / stepX).roundToInt().coerceIn(0, n - 1)
                }
                onPointSelected(points[index])
            }
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(tapModifier)) {
        val n = points.size
        if (n == 0) return@Canvas

        val width = size.width
        val height = size.height
        val prices = points.map { it.price }
        val averages = if (smaValues.size == n) smaValues else emptyList()
        val plotted = prices + averages.filterNotNull()
        val min = plotted.min()
        val max = plotted.max()
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0

        fun yOf(price: Double): Float {
            val norm = ((price - min) / range).toFloat().coerceIn(0f, 1f)
            val padded = 0.08f + norm * 0.84f
            return height - padded * height
        }

        fun xOf(index: Int): Float = if (n == 1) width / 2f else (width / (n - 1)) * index

        val strokeWidth = 2.dp.toPx()

        fun drawMarker() {
            val selectedIndex = selectedPoint?.let { points.indexOf(it) } ?: -1
            if (selectedIndex < 0) return
            val mx = xOf(selectedIndex)
            val my = yOf(points[selectedIndex].price)
            drawLine(
                color = guideColor,
                start = Offset(mx, 0f),
                end = Offset(mx, height),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(color = lineColor, radius = 6.dp.toPx(), center = Offset(mx, my))
            drawCircle(color = markerFill, radius = 3.dp.toPx(), center = Offset(mx, my))
        }

        fun drawSma() {
            if (averages.isEmpty()) return
            val smaPath = Path()
            var started = false
            averages.forEachIndexed { i, average ->
                if (average == null) {
                    started = false
                    return@forEachIndexed
                }
                val x = xOf(i)
                val y = yOf(average)
                if (started) {
                    smaPath.lineTo(x, y)
                } else {
                    smaPath.moveTo(x, y)
                    started = true
                }
            }
            val dashLength = 6.dp.toPx()
            drawPath(
                path = smaPath,
                color = smaColor,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(dashLength, dashLength),
                    ),
                ),
            )
        }

        if (n == 1) {
            val y = yOf(prices[0])
            drawLine(lineColor, Offset(0f, y), Offset(width, y), strokeWidth = strokeWidth)
            drawMarker()
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
        drawSma()
        drawMarker()
    }
}

@Composable
internal fun TimeFrameSelector(selected: ChartTimeFrame, onSelect: (ChartTimeFrame) -> Unit) {
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
