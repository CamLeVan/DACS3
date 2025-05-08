package com.example.taskapplication.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
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
        // Hiện tại chưa có repository đầy đủ, nên chưa thể tạo worker
        return null
    }
}
