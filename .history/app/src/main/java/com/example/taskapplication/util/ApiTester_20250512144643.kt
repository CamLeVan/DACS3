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
 * Lớp tiện ích để kiểm tra trực tiếp các endpoint API
 */
object ApiTester {
    private const val TAG = "ApiTester"
    private const val BASE_URL = "http://10.0.2.2:8000/api/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Test Google login endpoint
     */
    suspend fun testGoogleLogin(idToken: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("idToken", idToken)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BASE_URL}auth/google")
                    .post(requestBody)
                    .build()

                Log.d(TAG, "Testing Google login with token: ${idToken.take(10)}...")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                Log.d(TAG, "Google login response code: ${response.code}")
                Log.d(TAG, "Google login response: $responseBody")

                return@withContext "Response code: ${response.code}\nBody: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Error testing Google login", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }

    /**
     * Test login endpoint
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

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                Log.d(TAG, "Login response code: ${response.code}")
                Log.d(TAG, "Login response: $responseBody")

                return@withContext "Response code: ${response.code}\nBody: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Error testing login", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }

    /**
     * Test register endpoint
     */
    suspend fun testRegister(name: String, email: String, password: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("name", name)
                    put("email", email)
                    put("password", password)
                    put("passwordConfirmation", password)
                }

                val requestBody = json.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BASE_URL}auth/register")
                    .post(requestBody)
                    .build()

                Log.d(TAG, "Testing register with request: $json")

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: "No response body"

                Log.d(TAG, "Register response code: ${response.code}")
                Log.d(TAG, "Register response: $responseBody")

                return@withContext "Response code: ${response.code}\nBody: $responseBody"
            } catch (e: Exception) {
                Log.e(TAG, "Error testing register", e)
                return@withContext "Error: ${e.message}"
            }
        }
    }
}
