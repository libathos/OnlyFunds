package compose.demo.onlyfunds.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val Navy = Color(0xFF2B3A4A)
private val OffWhite = Color(0xFFECEFF4)
private val SkyBlue = Color(0xFF7FB2E5)
private val Mint = Color(0xFF8FD9A8)
private val DividerWhite = Color(0x33FFFFFF)
private val PositiveGreen = Color(0xFF4CD787)
private val NegativeRed = Color(0xFFE5737A)
private val SmaAmber = Color(0xFFF2C14E)

private val OnlyFundsColorScheme = darkColorScheme(
    primary = SkyBlue,
    onPrimary = Navy,
    background = Navy,
    onBackground = OffWhite,
    surface = Navy,
    onSurface = OffWhite,
)

@Immutable
data class OnlyFundsColors(
    val price: Color,
    val divider: Color,
    val positive: Color,
    val negative: Color,
    val sma: Color,
)

private val LocalOnlyFundsColors = staticCompositionLocalOf {
    OnlyFundsColors(
        price = Color.Unspecified,
        divider = Color.Unspecified,
        positive = Color.Unspecified,
        negative = Color.Unspecified,
        sma = Color.Unspecified,
    )
}

object OnlyFundsTheme {
    val colors: OnlyFundsColors
        @Composable
        @ReadOnlyComposable
        get() = LocalOnlyFundsColors.current
}

@Composable
fun OnlyFundsTheme(content: @Composable () -> Unit) {
    val colors = OnlyFundsColors(
        price = Mint,
        divider = DividerWhite,
        positive = PositiveGreen,
        negative = NegativeRed,
        sma = SmaAmber,
    )
    CompositionLocalProvider(LocalOnlyFundsColors provides colors) {
        MaterialTheme(
            colorScheme = OnlyFundsColorScheme,
            content = content,
        )
    }
}
