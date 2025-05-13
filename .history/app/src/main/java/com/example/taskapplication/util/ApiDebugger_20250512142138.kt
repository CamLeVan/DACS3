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

                Log.d(TAG, "Testing API connection to $BASE_URL")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                Log.d(TAG, "API connection response code: ${response.code}")
                Log.d(TAG, "API connection response: $responseBody")

                return@withContext "Response code: ${response.code}\nBody: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Error testing API connection", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }

    /**
     * Test the login endpoint with detailed logging
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

                Log.d(TAG, "Testing login with request: $json")
                Log.d(TAG, "Full request URL: ${request.url}")
                Log.d(TAG, "Request headers: ${request.headers}")
                Log.d(TAG, "Request method: ${request.method}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                Log.d(TAG, "Login response code: ${response.code}")
                Log.d(TAG, "Login response headers: ${response.headers}")
                Log.d(TAG, "Login response: $responseBody")

                return@withContext "Response code: ${response.code}\nHeaders: ${response.headers}\nBody: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Error testing login", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }

    /**
     * Test the register endpoint with detailed logging
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

                Log.d(TAG, "Testing register with request: $json")
                Log.d(TAG, "Full request URL: ${request.url}")
                Log.d(TAG, "Request headers: ${request.headers}")
                Log.d(TAG, "Request method: ${request.method}")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                Log.d(TAG, "Register response code: ${response.code}")
                Log.d(TAG, "Register response headers: ${response.headers}")
                Log.d(TAG, "Register response: $responseBody")

                return@withContext "Response code: ${response.code}\nHeaders: ${response.headers}\nBody: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Error testing register", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }

    /**
     * Test the Google login endpoint with detailed logging
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
