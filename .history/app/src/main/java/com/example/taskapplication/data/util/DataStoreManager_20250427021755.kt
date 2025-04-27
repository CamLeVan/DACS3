package com.example.taskapplication.data.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "task_app_preferences")

@Singleton
class DataStoreManager @Inject constructor(
    private val context: Context
) {
    // Keys
    private object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_NOTIFICATION_CHECK_TIMESTAMP = longPreferencesKey("last_notification_check_timestamp")
        val DEVICE_ID = stringPreferencesKey("device_id")
    }

    // Authentication preferences
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
        }
    }

    val authToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTH_TOKEN]
    }

    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REFRESH_TOKEN] = token
        }
    }

    val refreshToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REFRESH_TOKEN]
    }

    suspend fun saveTokenExpiresAt(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOKEN_EXPIRES_AT] = timestamp
        }
    }

    val tokenExpiresAt: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TOKEN_EXPIRES_AT]
    }

    // User information
    suspend fun saveUserId(userId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ID] = userId
        }
    }

    val userId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ID]
    }

    suspend fun saveUserInfo(name: String, email: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_NAME] = name
            preferences[PreferencesKeys.USER_EMAIL] = email
        }
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_NAME]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_EMAIL]
    }

    // Sync preferences
    suspend fun saveLastSyncTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP] = timestamp
        }
    }

    val lastSyncTimestamp: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_SYNC_TIMESTAMP]
    }

    // Device ID
    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVICE_ID] = deviceId
        }
    }

    val deviceId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEVICE_ID]
    }

    // Clear all preferences
    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}