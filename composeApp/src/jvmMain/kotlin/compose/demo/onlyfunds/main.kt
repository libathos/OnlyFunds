package compose.demo.onlyfunds

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import compose.demo.onlyfunds.application.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OnlyFunds",
    ) {
        App()
    }
}