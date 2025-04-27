package com.example.taskapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.InitialSyncRequest
import com.example.taskapplication.data.api.request.PushChangesRequest
import com.example.taskapplication.data.api.request.QuickSyncRequest
import com.example.taskapplication.data.database.dao.*
import com.example.taskapplication.data.database.entities.SyncMetadataEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.workers.SyncWorker
import com.example.taskapplication.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val syncMetadataDao: SyncMetadataDao,
    private val personalTaskDao: PersonalTaskDao,
    private val teamTaskDao: TeamTaskDao,
    private val messageDao: MessageDao,
    private val messageReadStatusDao: MessageReadStatusDao,
    private val messageReactionDao: MessageReactionDao,
    private val userDao: UserDao,
    private val teamDao: TeamDao,
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker,
    private val context: Context
) : SyncRepository {

    private val TAG = "SyncRepository"

    override suspend fun initialSync(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val deviceId = getOrCreateDeviceId()

            val response = apiService.initialSync(InitialSyncRequest(deviceId))
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(IOException("Initial sync failed: ${response.message()}"))
            }

            val initialSyncData = response.body()!!

            // Process and save data to Room
            // This is a simplified implementation

            // Save sync timestamp
            updateLastSyncTimestamp(initialSyncData.timestamp)

            Log.d(TAG, "Initial sync completed successfully")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial sync", e)
            return Result.failure(e)
        }
    }

    override suspend fun quickSync(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val lastSyncTimestamp = getLastSyncTimestamp() ?: 0
            val deviceId = getOrCreateDeviceId()

            val response = apiService.quickSync(QuickSyncRequest(lastSyncTimestamp, deviceId))
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(IOException("Quick sync failed: ${response.message()}"))
            }

            val syncData = response.body()!!

            // Process and save data to Room
            // This is a simplified implementation

            // Save sync timestamp
            updateLastSyncTimestamp(syncData.timestamp)

            Log.d(TAG, "Quick sync completed successfully")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during quick sync", e)
            return Result.failure(e)
        }
    }

    override suspend fun pushChanges(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }

        try {
            val deviceId = getOrCreateDeviceId()

            // Collect pending tasks from each DAO
            val pendingPersonalTasks = personalTaskDao.getPendingSyncTasks()
            val pendingTeamTasks = teamTaskDao.getPendingSyncTasks()
            val pendingMessages = messageDao.getPendingSyncMessages()
            val pendingReadStatuses = messageReadStatusDao.getPendingSyncReadStatuses()
            val pendingReactions = messageReactionDao.getPendingSyncReactions()

            // Create push request with pending changes
            val pushRequest = PushChangesRequest(
                deviceId = deviceId,
                // TODO: Convert entities to push data format
                personalTasks = null, // Convert pendingPersonalTasks
                teamTasks = null, // Convert pendingTeamTasks
                messageReadStatuses = null, // Convert pendingReadStatuses
                messageReactions = null // Convert pendingReactions
            )

            val response = apiService.pushChanges(pushRequest)
            if (!response.isSuccessful || response.body() == null) {
                return Result.failure(IOException("Push changes failed: ${response.message()}"))
            }

            val responseBody = response.body()!!

            // Process push response
            if (responseBody.success) {
                // Update local entities with server IDs
                // TODO: Update entities with server IDs from responseBody.personalTasks, etc.

                // Handle conflicts if any
                responseBody.conflicts?.let { conflicts ->
                    if (conflicts.isNotEmpty()) {
                        Log.w(TAG, "Sync conflicts detected: ${conflicts.size}")
                        // TODO: Handle conflicts based on conflict resolution strategy
                    }
                }

                // Update sync timestamp
                responseBody.timestamp.let { timestamp ->
                    updateLastSyncTimestamp(timestamp)
                }
            } else {
                return Result.failure(IOException("Push changes failed: Server reported failure"))
            }

            Log.d(TAG, "Push changes completed successfully")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error during push changes", e)
            return Result.failure(e)
        }
    }

    override suspend fun hasPendingChanges(): Boolean {
        try {
            val pendingPersonalTasks = personalTaskDao.getPendingSyncTasks()
            val pendingTeamTasks = teamTaskDao.getPendingSyncTasks()
            val pendingMessages = messageDao.getPendingSyncMessages()
            val pendingReadStatuses = messageReadStatusDao.getPendingSyncReadStatuses()
            val pendingReactions = messageReactionDao.getPendingSyncReactions()

            return pendingPersonalTasks.isNotEmpty() ||
                   pendingTeamTasks.isNotEmpty() ||
                   pendingMessages.isNotEmpty() ||
                   pendingReadStatuses.isNotEmpty() ||
                   pendingReactions.isNotEmpty()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking pending changes", e)
            return false
        }
    }

    override suspend fun getLastSyncTimestamp(): Long? {
        // Try from Room first
        val syncMetadata = syncMetadataDao.getSyncMetadata("global")
        if (syncMetadata != null) {
            return syncMetadata.lastSyncTimestamp
        }

        // Fall back to DataStore
        return dataStoreManager.lastSyncTimestamp.first()
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        // Update in Room
        val syncMetadata = SyncMetadataEntity(
            id = "global",
            lastSyncTimestamp = timestamp,
            entityType = "global"
        )
        syncMetadataDao.insertSyncMetadata(syncMetadata)

        // Also update in DataStore for backup
        dataStoreManager.saveLastSyncTimestamp(timestamp)
    }

    override suspend fun schedulePeriodicSync(intervalMinutes: Int) {
        SyncWorker.schedulePeriodicSync(context, intervalMinutes)
    }

    override suspend fun cancelPeriodicSync() {
        SyncWorker.cancelPeriodicSync(context)
    }

    private suspend fun getOrCreateDeviceId(): String {
        var deviceId = dataStoreManager.deviceId.first()
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            dataStoreManager.saveDeviceId(deviceId)
        }
        return deviceId
    }
}