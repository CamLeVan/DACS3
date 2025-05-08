package com.example.taskapplication.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.taskapplication.data.datastore.DataStoreManager
import com.example.taskapplication.workers.NotificationWorker
import com.example.taskapplication.workers.SyncWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskWorkerFactory @Inject constructor(
    private val dataStoreManager: DataStoreManager
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SyncWorker::class.java.name -> SyncWorker(appContext, workerParameters, dataStoreManager)
            NotificationWorker::class.java.name -> NotificationWorker(appContext, workerParameters, dataStoreManager)
            else -> null
        }
    }
}
