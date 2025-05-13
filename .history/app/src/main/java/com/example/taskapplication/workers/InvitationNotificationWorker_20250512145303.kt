package com.example.taskapplication.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.taskapplication.R
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.model.TeamInvitation
import com.example.taskapplication.domain.repository.TeamInvitationRepository
import com.example.taskapplication.ui.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker để kiểm tra và hiển thị thông báo về lời mời mới
 */
@HiltWorker
class InvitationNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val teamInvitationRepository: TeamInvitationRepository,
    private val connectionChecker: ConnectionChecker,
    private val dataStoreManager: DataStoreManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "InvitationNotificationWorker"
        private const val INVITATION_WORK_NAME = "invitation_notification_polling"
        private const val CHANNEL_ID = "team_invitations"
        private const val NOTIFICATION_GROUP = "team_invitations_group"

        /**
         * Lên lịch kiểm tra lời mời định kỳ
         */
        fun schedulePeriodicInvitationCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<InvitationNotificationWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    INVITATION_WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )

            Log.d(TAG, "Đã lên lịch kiểm tra lời mời định kỳ")
        }

        /**
         * Tạo kênh thông báo cho lời mời
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.invitation_channel_name)
                val descriptionText = context.getString(R.string.invitation_channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(TAG, "Đang kiểm tra lời mời mới")

        // Kiểm tra kết nối mạng
        if (!connectionChecker.isNetworkAvailable()) {
            Log.d(TAG, "Không có kết nối mạng, thử lại sau")
            return@withContext Result.retry()
        }

        try {
            // Lấy email người dùng hiện tại
            val email = dataStoreManager.userEmail.first()
            if (email.isNullOrEmpty()) {
                Log.d(TAG, "Không tìm thấy email người dùng, không thể kiểm tra lời mời")
                return@withContext Result.failure()
            }

            // Đồng bộ lời mời từ server
            teamInvitationRepository.syncInvitations()

            // Lấy danh sách lời mời đang chờ
            val invitations = teamInvitationRepository.getUserInvitations().first()
                .filter { it.status == "pending" }

            if (invitations.isNotEmpty()) {
                Log.d(TAG, "Tìm thấy ${invitations.size} lời mời đang chờ")
                showInvitationNotifications(invitations)
            } else {
                Log.d(TAG, "Không tìm thấy lời mời đang chờ nào")
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kiểm tra lời mời", e)
            return@withContext if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    /**
     * Hiển thị thông báo cho các lời mời
     */
    private fun showInvitationNotifications(invitations: List<TeamInvitation>) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo intent để mở ứng dụng
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_INVITATIONS", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hiển thị thông báo cho mỗi lời mời
        invitations.forEachIndexed { index, invitation ->
            val notificationId = invitation.id.hashCode()

            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(applicationContext.getString(R.string.invitation_title))
                .setContentText(applicationContext.getString(R.string.invitation_content, invitation.teamName))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup(NOTIFICATION_GROUP)
                .build()

            notificationManager.notify(notificationId, notification)
        }

        // Nếu có nhiều hơn 1 lời mời, hiển thị thông báo tóm tắt
        if (invitations.size > 1) {
            val summaryNotification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(applicationContext.getString(R.string.invitation_summary_title, invitations.size))
                .setContentText(applicationContext.getString(R.string.invitation_summary_content, invitations.size))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setGroup(NOTIFICATION_GROUP)
                .setGroupSummary(true)
                .build()

            notificationManager.notify(0, summaryNotification)
        }
    }
}
