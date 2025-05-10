package com.example.taskapplication.util

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class ApiResponse<out T> {
    data class Success<out T>(val data: T) : ApiResponse<T>()
    data class Error(val message: String, val code: Int = 0) : ApiResponse<Nothing>()
    object Loading : ApiResponse<Nothing>()

    companion object {
        fun <T> success(data: T): ApiResponse<T> = Success(data)
        fun error(message: String, code: Int = 0): ApiResponse<Nothing> = Error(message, code)
        fun loading(): ApiResponse<Nothing> = Loading
    }
}
