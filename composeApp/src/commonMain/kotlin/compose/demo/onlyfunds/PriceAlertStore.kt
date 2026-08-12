package compose.demo.onlyfunds

import io.onlyfunds.domain.model.PriceAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory store for user-defined price alerts, shared across screens.
 *
 * An alert is created on the chart screen but evaluated by the top-stocks
 * poller, so both need to reach the same source of truth. Alerts are keyed by
 * symbol; setting a new alert for a symbol replaces the previous one.
 */
object PriceAlertStore {
    private val _alerts = MutableStateFlow<Map<String, PriceAlert>>(emptyMap())
    val alerts: StateFlow<Map<String, PriceAlert>> = _alerts.asStateFlow()

    fun setAlert(alert: PriceAlert) {
        _alerts.update { it + (alert.symbol to alert) }
    }

    fun remove(symbol: String) {
        _alerts.update { it - symbol }
    }
}
