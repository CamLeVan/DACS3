package com.example.taskapplication.util

import com.example.taskapplication.data.remote.dto.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Safe API call utility function
 */
suspend fun <T> safeApiCall(
    apiCall: suspend () -> T
): ApiResponse<T> {
    return withContext(Dispatchers.IO) {
        try {
            ApiResponse.Success(apiCall.invoke())
        } catch (throwable: Throwable) {
            when (throwable) {
                is IOException -> ApiResponse.Error("Network Error: ${throwable.message}", 0)
                is HttpException -> {
                    val code = throwable.code()
                    val errorResponse = throwable.message()
                    ApiResponse.Error("Error $code: $errorResponse", code)
                }
                else -> ApiResponse.Error("Unknown Error: ${throwable.message}", 0)
            }
        }
    }
}
