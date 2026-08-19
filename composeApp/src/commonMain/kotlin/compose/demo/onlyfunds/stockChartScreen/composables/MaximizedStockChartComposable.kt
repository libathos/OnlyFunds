package compose.demo.onlyfunds.stockChartScreen.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import compose.demo.onlyfunds.application.misc.LockScreenOrientation
import compose.demo.onlyfunds.application.misc.ScreenOrientation
import compose.demo.onlyfunds.stockChartScreen.mvi.ChartPoint
import compose.demo.onlyfunds.stockChartScreen.mvi.StockChartUiState
import compose.demo.onlyfunds.theme.OnlyFundsTheme
import compose.demo.onlyfunds.topStocksScreen.mvi.PriceTrend
import io.onlyfunds.domain.model.ChartTimeFrame

@Composable
fun MaximizedStockChartDialog(
    symbol: String,
    content: StockChartUiState.Content.Chart,
    selectedTimeFrame: ChartTimeFrame,
    onSelectTimeFrame: (ChartTimeFrame) -> Unit,
    onDismiss: () -> Unit,
) {
    LockScreenOrientation(ScreenOrientation.LANDSCAPE)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        MaximizedStockChartContent(
            symbol = symbol,
            content = content,
            selectedTimeFrame = selectedTimeFrame,
            onSelectTimeFrame = onSelectTimeFrame,
            onMinimize = onDismiss,
        )
    }
}

@Composable
internal fun MaximizedStockChartContent(
    symbol: String,
    content: StockChartUiState.Content.Chart,
    selectedTimeFrame: ChartTimeFrame,
    onSelectTimeFrame: (ChartTimeFrame) -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSma by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.width(16.dp))
            PriceSummaryInline(
                latestPrice = content.latestPrice,
                changeLabel = content.changeLabel,
                trend = content.trend,
            )
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onMinimize)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                MinimizeGlyph(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        TimeFrameSelector(selected = selectedTimeFrame, onSelect = onSelectTimeFrame)

        Spacer(Modifier.height(12.dp))

        LineChart(
            points = content.points,
            trend = content.trend,
            selectedPoint = null,
            selectionEnabled = false,
            onPointSelected = {},
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
private fun PriceSummaryInline(latestPrice: String, changeLabel: String, trend: PriceTrend) {
    val changeColor = when (trend) {
        PriceTrend.Up -> OnlyFundsTheme.colors.positive
        PriceTrend.Down -> OnlyFundsTheme.colors.negative
        PriceTrend.Flat -> MaterialTheme.colorScheme.onBackground
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = latestPrice,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = OnlyFundsTheme.colors.price,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = changeLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = changeColor,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
internal fun MaximizeGlyph(color: Color, modifier: Modifier = Modifier) {
    CornerBracketsGlyph(color = color, inward = false, modifier = modifier)
}

@Composable
internal fun MinimizeGlyph(color: Color, modifier: Modifier = Modifier) {
    CornerBracketsGlyph(color = color, inward = true, modifier = modifier)
}

@Composable
private fun CornerBracketsGlyph(color: Color, inward: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = 2.dp.toPx()
        val inset = if (inward) size.minDimension * 0.28f else stroke / 2f
        val armLength = size.minDimension * 0.28f
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        val path = Path().apply {
            moveTo(left, top + armLength)
            lineTo(left, top)
            lineTo(left + armLength, top)

            moveTo(right - armLength, top)
            lineTo(right, top)
            lineTo(right, top + armLength)

            moveTo(right, bottom - armLength)
            lineTo(right, bottom)
            lineTo(right - armLength, bottom)

            moveTo(left + armLength, bottom)
            lineTo(left, bottom)
            lineTo(left, bottom - armLength)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        if (inward) {
            val diagonal = size.minDimension * 0.18f
            drawLine(
                color = color,
                start = Offset(left - diagonal, top - diagonal),
                end = Offset(left, top),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = color,
                start = Offset(right + diagonal, bottom + diagonal),
                end = Offset(right, bottom),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Preview
@Composable
fun MaximizedStockChartPreview() {
    OnlyFundsTheme {
        MaximizedStockChartContent(
            symbol = "AAPL",
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
            selectedTimeFrame = ChartTimeFrame.MONTH,
            onSelectTimeFrame = {},
            onMinimize = {},
        )
    }
}
