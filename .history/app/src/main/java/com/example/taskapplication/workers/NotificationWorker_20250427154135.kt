package com.example.taskapplication.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.taskapplication.R
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.repository.NotificationRepository
import com.example.taskapplication.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class NotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val connectionChecker: ConnectionChecker,
    private val dataStoreManager: DataStoreManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "NotificationWorker"
        private const val NOTIFICATION_WORK_NAME = "notification_polling"
        private const val CHANNEL_ID = "task_app_notifications"

        fun schedulePeriodicNotifications(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<NotificationWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    NOTIFICATION_WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )

            Log.d(TAG, "Periodic notification polling scheduled")
        }

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Task App Notifications"
                val descriptionText = "Notifications for messages, tasks, and other updates"
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)

                Log.d(TAG, "Notification channel created")
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Starting notification polling")

        // Check if we have internet connection
        if (!connectionChecker.isNetworkAvailable()) {
            Log.d(TAG, "No network connection, retrying later")
            return@withContext Result.retry()
        }

        try {
            val lastChecked = dataStoreManager.getLastNotificationCheckTimestamp()
            val deviceId = dataStoreManager.getDeviceId()

            if (deviceId.isEmpty()) {
                Log.e(TAG, "Device ID not found")
                return@withContext Result.failure()
            }

            val result = notificationRepository.getNotifications(lastChecked)

            if (result.isSuccess) {
                val notifications = result.getOrNull() ?: emptyList()
                Log.d(TAG, "Received ${notifications.size} notifications")

                notifications.forEach { notification ->
                    showNotification(notification)
                }

                // Update last check time
                if (notifications.isNotEmpty()) {
                    dataStoreManager.saveLastNotificationCheckTimestamp(System.currentTimeMillis())
                }

                return@withContext Result.success()
            } else {
                Log.e(TAG, "Failed to fetch notifications", result.exceptionOrNull())
                return@withContext if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during notification polling", e)
            return@withContext if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun showNotification(notification: com.example.taskapplication.domain.model.Notification) {
        val notificationId = notification.id.hashCode()

        // Create intent based on notification type
        val intent = when (notification.type) {
            "new_message" -> {
                val teamId = notification.data["team_id"] as? String ?: return
                Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra("DESTINATION", "chat")
                    putExtra("TEAM_ID", teamId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
            "task_assignment" -> {
                val teamId = notification.data["team_id"] as? String ?: return
                val taskId = notification.data["task_id"] as? String ?: return
                Intent(applicationContext, MainActivity::class.java).apply {
                    putExtra("DESTINATION", "team_task")
                    putExtra("TEAM_ID", teamId)
                    putExtra("TASK_ID", taskId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
            else -> {
                Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(applicationContext)

        // Check for notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                notificationManager.notify(notificationId, builder.build())
            }
        } else {
            // For versions below Android 13, permission is granted at install time
            notificationManager.notify(notificationId, builder.build())
        }
    }
}
