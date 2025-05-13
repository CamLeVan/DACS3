package com.example.taskapplication.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Lớp tiện ích để gỡ lỗi các vấn đề API
 */
object ApiDebugger {
    private const val TAG = "ApiDebugger"
    private const val BASE_URL = "http://10.0.2.2:8000/api/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Kiểm tra kết nối API
     */
    suspend fun testConnection(): String {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .get()
                    .build()

                Log.d(TAG, "Đang kiểm tra kết nối API đến $BASE_URL")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "Không có nội dung phản hồi"

                Log.d(TAG, "Mã phản hồi kết nối API: ${response.code}")
                Log.d(TAG, "Phản hồi kết nối API: $responseBody")

                return@withContext "Mã phản hồi: ${response.code}\nNội dung: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi kiểm tra kết nối API", e)
                return@withContext "Lỗi: ${e.message}"
            }
        }
    }

    /**
     * Kiểm tra endpoint đăng nhập với ghi log chi tiết
     */
    suspend fun testLogin(email: String, password: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BASE_URL}auth/login")
                    .post(requestBody)
                    .build()

                Log.d(TAG, "Đang kiểm tra đăng nhập với yêu cầu: $json")
                Log.d(TAG, "URL yêu cầu đầy đủ: ${request.url}")
                Log.d(TAG, "Headers yêu cầu: ${request.headers}")
                Log.d(TAG, "Phương thức yêu cầu: ${request.method}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "Không có nội dung phản hồi"

                Log.d(TAG, "Mã phản hồi đăng nhập: ${response.code}")
                Log.d(TAG, "Headers phản hồi đăng nhập: ${response.headers}")
                Log.d(TAG, "Phản hồi đăng nhập: $responseBody")

                return@withContext "Mã phản hồi: ${response.code}\nHeaders: ${response.headers}\nNội dung: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi kiểm tra đăng nhập", e)
                return@withContext "Lỗi: ${e.message}"
            }
        }
    }

    /**
     * Kiểm tra endpoint đăng ký với ghi log chi tiết
     */
    suspend fun testRegister(name: String, email: String, password: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("password", password)
                    put("password_confirmation", password)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BASE_URL}auth/register")
                    .post(requestBody)
                    .build()

                Log.d(TAG, "Đang kiểm tra đăng ký với yêu cầu: $json")
                Log.d(TAG, "URL yêu cầu đầy đủ: ${request.url}")
                Log.d(TAG, "Headers yêu cầu: ${request.headers}")
                Log.d(TAG, "Phương thức yêu cầu: ${request.method}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "Không có nội dung phản hồi"

                Log.d(TAG, "Mã phản hồi đăng ký: ${response.code}")
                Log.d(TAG, "Headers phản hồi đăng ký: ${response.headers}")
                Log.d(TAG, "Phản hồi đăng ký: $responseBody")

                return@withContext "Mã phản hồi: ${response.code}\nHeaders: ${response.headers}\nNội dung: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi khi kiểm tra đăng ký", e)
                return@withContext "Lỗi: ${e.message}"
            }
        }
    }

    /**
     * Kiểm tra endpoint đăng nhập Google với ghi log chi tiết
     */
    suspend fun testGoogleLogin(idToken: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("id_token", idToken)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BASE_URL}auth/google")
                    .post(requestBody)
                    .build()

                Log.d(TAG, "Testing Google login with token: ${idToken.take(10)}...")
                Log.d(TAG, "Full request URL: ${request.url}")
                Log.d(TAG, "Request headers: ${request.headers}")
                Log.d(TAG, "Request method: ${request.method}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                Log.d(TAG, "Google login response code: ${response.code}")
                Log.d(TAG, "Google login response headers: ${response.headers}")
                Log.d(TAG, "Google login response: $responseBody")

                return@withContext "Response code: ${response.code}\nHeaders: ${response.headers}\nBody: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Error testing Google login", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }
}
