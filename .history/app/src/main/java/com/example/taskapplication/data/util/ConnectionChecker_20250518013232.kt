package com.example.taskapplication.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionChecker @Inject constructor(
    private val context: Context
) {
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork
        if (networkCapabilities == null) {
            Log.d("ConnectionChecker", "No active network")
            return false
        }

        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities)
        if (activeNetwork == null) {
            Log.d("ConnectionChecker", "No network capabilities")
            return false
        }

        val isAvailable = when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                Log.d("ConnectionChecker", "WIFI connection available")
                true
            }
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                Log.d("ConnectionChecker", "CELLULAR connection available")
                true
            }
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
                Log.d("ConnectionChecker", "ETHERNET connection available")
                true
            }
            else -> {
                Log.d("ConnectionChecker", "No suitable network transport available")
                false
            }
        }

        Log.d("ConnectionChecker", "Network available: $isAvailable")
        return isAvailable
    }
}