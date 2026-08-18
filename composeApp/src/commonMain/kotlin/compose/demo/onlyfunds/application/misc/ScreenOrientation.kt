package compose.demo.onlyfunds.application.misc

import androidx.compose.runtime.Composable

enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE,
}

@Composable
expect fun LockScreenOrientation(orientation: ScreenOrientation)
