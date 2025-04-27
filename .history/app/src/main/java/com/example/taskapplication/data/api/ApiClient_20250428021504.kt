package com.example.taskapplication.data.api

import com.example.taskapplication.data.util.AuthInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is a helper for direct API access without going through the repository layer.
 * Normally you should use Repository classes, but this can be useful for quick prototyping
 * or for components that don't need the full repository functionality.
 */
@Singleton
class ApiClient @Inject constructor(
    private val authInterceptor: AuthInterceptor
) {

    companion object {
        private const val BASE_URL = "http://10.0.2.2:8000/api/" // Android emulator uses 10.0.2.2 to access host's localhost
        private const val TIMEOUT = 30L
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}