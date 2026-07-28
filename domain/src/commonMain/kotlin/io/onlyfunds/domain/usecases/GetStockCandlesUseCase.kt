package io.onlyfunds.domain.usecases

import io.onlyfunds.domain.mapper.toDomain
import io.onlyfunds.domain.model.CandleResolution
import io.onlyfunds.domain.model.ChartTimeFrame
import io.onlyfunds.domain.model.StockCandles
import io.onlyfunds.network.CandleService
import io.onlyfunds.network.NetworkResponse

class GetStockCandlesUseCase(
    private val candleService: CandleService = CandleService(),
) {
    suspend operator fun invoke(
        symbol: String,
        resolution: CandleResolution,
        from: Long,
        to: Long,
    ): NetworkResponse<StockCandles> =
        when (val response = candleService.getCandles(symbol, resolution.apiValue, from, to)) {
            is NetworkResponse.Success ->
                NetworkResponse.Success(response.data.toDomain(symbol), response.statusCode)

            is NetworkResponse.Error -> response
        }

    suspend operator fun invoke(
        symbol: String,
        timeFrame: ChartTimeFrame,
        nowEpochSeconds: Long,
    ): NetworkResponse<StockCandles> {
        val range = timeFrame.toRange(nowEpochSeconds)
        return invoke(symbol, timeFrame.resolution, range.from, range.to)
    }
}
