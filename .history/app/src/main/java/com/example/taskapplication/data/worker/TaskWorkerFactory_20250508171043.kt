package com.example.taskapplication.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import com.example.taskapplication.domain.repository.NotificationRepository
import com.example.taskapplication.domain.repository.SyncRepository
import com.example.taskapplication.workers.NotificationWorker
import com.example.taskapplication.workers.SyncWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskWorkerFactory @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val connectionChecker: ConnectionChecker
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        // Since we've removed the repositories, we'll return null for now
        // This will prevent the workers from being created
        return null
    }
}
