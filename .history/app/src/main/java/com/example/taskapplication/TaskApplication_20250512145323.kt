package com.example.taskapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import com.example.taskapplication.R
import com.example.taskapplication.workers.InvitationNotificationWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaskApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.work.WorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        scheduleWorkers()
    }

    /**
     * Lên lịch cho các worker
     */
    private fun scheduleWorkers() {
        // Lên lịch kiểm tra lời mời
        InvitationNotificationWorker.schedulePeriodicInvitationCheck(this)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Kênh nhắc nhở công việc
            val taskReminderChannel = NotificationChannel(
                CHANNEL_TASK_REMINDER,
                "Nhắc nhở công việc",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở công việc"
                enableVibration(true)
            }

            // Team Message Channel
            val teamMessageChannel = NotificationChannel(
                CHANNEL_TEAM_MESSAGE,
                "Team Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for team messages"
                enableVibration(true)
            }

            // Task Assignment Channel
            val taskAssignmentChannel = NotificationChannel(
                CHANNEL_TASK_ASSIGNMENT,
                "Task Assignments",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for task assignments"
                enableVibration(true)
            }

            // Team Invitation Channel
            val teamInvitationChannel = NotificationChannel(
                CHANNEL_TEAM_INVITATION,
                getString(R.string.invitation_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.invitation_channel_description)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    taskReminderChannel,
                    teamMessageChannel,
                    taskAssignmentChannel,
                    teamInvitationChannel
                )
            )
        }
    }



    companion object {
        const val CHANNEL_TASK_REMINDER = "task_reminder"
        const val CHANNEL_TEAM_MESSAGE = "team_message"
        const val CHANNEL_TASK_ASSIGNMENT = "task_assignment"
        const val CHANNEL_TEAM_INVITATION = "team_invitations"
    }
}
