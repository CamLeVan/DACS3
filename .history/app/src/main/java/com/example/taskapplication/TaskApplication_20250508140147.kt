package com.example.taskapplication

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TaskApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: androidx.work.WorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Task Reminder Channel
            val taskReminderChannel = NotificationChannel(
                CHANNEL_TASK_REMINDER,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task reminders"
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

            notificationManager.createNotificationChannels(
                listOf(
                    taskReminderChannel,
                    teamMessageChannel,
                    taskAssignmentChannel
                )
            )
        }
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    companion object {
        const val CHANNEL_TASK_REMINDER = "task_reminder"
        const val CHANNEL_TEAM_MESSAGE = "team_message"
        const val CHANNEL_TASK_ASSIGNMENT = "task_assignment"
    }
} 