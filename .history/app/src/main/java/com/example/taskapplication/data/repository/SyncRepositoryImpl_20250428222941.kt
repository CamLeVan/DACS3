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
import com.example.taskapplication.domain.repository.SyncRepository
import androidx.work.*
import java.util.concurrent.TimeUnit
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
            // TODO: Implement initialSync in ApiService
            // Khi triển khai API, bỏ comment các dòng dưới đây
            // val deviceId = getOrCreateDeviceId()
            // val response = apiService.initialSync(InitialSyncRequest(deviceId))
            // if (!response.isSuccessful || response.body() == null) {
            //     return Result.failure(IOException("Initial sync failed: ${response.message()}"))
            // }
            //
            // val initialSyncData = response.body()!!

            // Process and save data to Room
            try {
                // Save user data
                // initialSyncData.user.let { userResponse ->
                //     // TODO: Convert and save user data
                //     Log.d(TAG, "Saving user data")
                // }

                // Save teams data
                // initialSyncData.teams.let { teams ->
                //     // TODO: Convert and save teams
                //     Log.d(TAG, "Saving ${teams.size} teams")
                // }

                // Save team members data
                // initialSyncData.teamMembers.let { teamMembers ->
                //     // TODO: Convert and save team members
                //     Log.d(TAG, "Saving ${teamMembers.size} team members")
                // }

                // Save personal tasks
                // initialSyncData.personalTasks.let { tasks ->
                //     // TODO: Convert and save personal tasks
                //     Log.d(TAG, "Saving ${tasks.size} personal tasks")
                // }

                // Save team tasks
                // initialSyncData.teamTasks.let { tasks ->
                //     // TODO: Convert and save team tasks
                //     Log.d(TAG, "Saving ${tasks.size} team tasks")
                // }

                // Save messages if available
                // initialSyncData.messages?.let { messages ->
                //     // TODO: Convert and save messages
                //     Log.d(TAG, "Saving ${messages.size} messages")
                // }

                // Save sync timestamp
                updateLastSyncTimestamp(System.currentTimeMillis())

                Log.d(TAG, "Initial sync completed successfully")
                return Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing initial sync data", e)
                return Result.failure(e)
            }
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
            // TODO: Implement quickSync in ApiService
            // Khi triển khai API, bỏ comment các dòng dưới đây
            // val lastSyncTimestamp = getLastSyncTimestamp() ?: 0
            // val deviceId = getOrCreateDeviceId()
            // val response = apiService.quickSync(QuickSyncRequest(lastSyncTimestamp, deviceId))
            // if (!response.isSuccessful || response.body() == null) {
            //     return Result.failure(IOException("Quick sync failed: ${response.message()}"))
            // }
            //
            // val syncData = response.body()!!

            // Process and save data to Room
            try {
                // Process users if available
                // syncData.users?.let { users ->
                //     // TODO: Process and save users
                //     Log.d(TAG, "Processing ${users.size} users")
                // }

                // Process teams if available
                // syncData.teams?.let { teamChanges ->
                //     // Process created teams
                //     teamChanges["created"]?.let { createdTeams ->
                //         // TODO: Save created teams
                //         Log.d(TAG, "Processing ${createdTeams.size} created teams")
                //     }
                //
                //     // Process updated teams
                //     teamChanges["updated"]?.let { updatedTeams ->
                //         // TODO: Update existing teams
                //         Log.d(TAG, "Processing ${updatedTeams.size} updated teams")
                //     }
                //
                //     // Process deleted teams
                //     teamChanges["deleted"]?.let { deletedTeams ->
                //         // TODO: Delete teams
                //         Log.d(TAG, "Processing ${deletedTeams.size} deleted teams")
                //     }
                // }

                // Process team members if available
                // syncData.teamMembers?.let { teamMemberChanges ->
                //     // TODO: Process team member changes (created, updated, deleted)
                //     Log.d(TAG, "Processing team member changes")
                // }

                // Process personal tasks if available
                // syncData.personalTasks?.let { taskChanges ->
                //     // TODO: Process personal task changes (created, updated, deleted)
                //     Log.d(TAG, "Processing personal task changes")
                // }

                // Process team tasks if available
                // syncData.teamTasks?.let { taskChanges ->
                //     // TODO: Process team task changes (created, updated, deleted)
                //     Log.d(TAG, "Processing team task changes")
                // }

                // Process messages if available
                // syncData.messages?.let { messageChanges ->
                //     // TODO: Process message changes (created, updated, deleted)
                //     Log.d(TAG, "Processing message changes")
                // }

                // Save sync timestamp
                updateLastSyncTimestamp(System.currentTimeMillis())

                Log.d(TAG, "Quick sync completed successfully")
                return Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing quick sync data", e)
                return Result.failure(e)
            }
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
            // Kiểm tra xem có thay đổi nào cần đồng bộ không
            if (hasPendingChanges()) {
                Log.d(TAG, "Có thay đổi cần đồng bộ")

                // Lấy deviceId để sử dụng trong API call
                val deviceId = getOrCreateDeviceId()

                // TODO: Implement pushChanges in ApiService
                // val pushRequest = PushChangesRequest(
                //     deviceId = deviceId,
                //     // TODO: Convert entities to push data format
                //     personalTasks = null,
                //     teamTasks = null,
                //     messageReadStatuses = null,
                //     messageReactions = null
                // )
            }

            // TODO: Implement pushChanges in ApiService
            // val response = apiService.pushChanges(pushRequest)
            // if (!response.isSuccessful || response.body() == null) {
            //     return Result.failure(IOException("Push changes failed: ${response.message()}"))
            // }
            //
            // val responseBody = response.body()!!

            // Process push response
            // if (responseBody.success) {
            //     // Update local entities with server IDs
            //     // TODO: Update entities with server IDs from responseBody.personalTasks, etc.
            //
            //     // Handle conflicts if any
            //     responseBody.conflicts?.let { conflicts ->
            //         if (conflicts.isNotEmpty()) {
            //             Log.w(TAG, "Sync conflicts detected: ${conflicts.size}")
            //             // TODO: Handle conflicts based on conflict resolution strategy
            //         }
            //     }
            //
            //     // Update sync timestamp
            //     responseBody.timestamp.let { timestamp ->
            //         updateLastSyncTimestamp(timestamp)
            //     }
            // } else {
            //     return Result.failure(IOException("Push changes failed: Server reported failure"))
            // }

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

            // Kiểm tra nếu có bất kỳ thay đổi nào đang chờ xử lý
            return pendingPersonalTasks.isNotEmpty() ||
                   pendingTeamTasks.isNotEmpty() ||
                   pendingMessages.isNotEmpty()
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
        try {
            // Tự triển khai schedulePeriodicSync thay vì sử dụng SyncWorker
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<com.example.taskapplication.workers.SyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "periodic_sync",
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWorkRequest
            )

            Log.d(TAG, "Scheduled periodic sync every $intervalMinutes minutes")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling periodic sync", e)
        }
    }

    override suspend fun cancelPeriodicSync() {
        try {
            // Tự triển khai cancelPeriodicSync thay vì sử dụng SyncWorker
            WorkManager.getInstance(context).cancelUniqueWork("periodic_sync")
            Log.d(TAG, "Cancelled periodic sync")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling periodic sync", e)
        }
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
