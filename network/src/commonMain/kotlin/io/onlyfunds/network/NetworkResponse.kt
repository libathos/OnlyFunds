package io.onlyfunds.network

sealed interface NetworkResponse<out T> {
    data class Success<out T>(val data: T, val statusCode: Int) : NetworkResponse<T>
    data class Error(val statusCode: Int, val message: String) : NetworkResponse<Nothing>
}
