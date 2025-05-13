package com.example.taskapplication.data.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.taskapplication.domain.repository.PersonalTaskRepository
import com.example.taskapplication.domain.repository.TeamTaskRepository
import com.example.taskapplication.domain.repository.MessageRepository
import com.example.taskapplication.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncRepository: SyncRepository,
    private val personalTaskRepository: PersonalTaskRepository,
    private val teamTaskRepository: TeamTaskRepository,
    private val messageRepository: MessageRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val SYNC_WORK_NAME = "sync_work"

        fun schedulePeriodicSync(context: Context, intervalMinutes: Int) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes.toLong(), TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 10, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                syncWorkRequest
            )

            Log.d(TAG, "Đã lên lịch đồng bộ định kỳ mỗi $intervalMinutes phút")
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SYNC_WORK_NAME)
            Log.d(TAG, "Đã hủy đồng bộ định kỳ")
        }

        fun runImmediateSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncWorkRequest)
            Log.d(TAG, "Đã bắt đầu đồng bộ ngay lập tức")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Đang bắt đầu công việc đồng bộ")

        try {
            // Đầu tiên đẩy các thay đổi cục bộ lên server
            syncRepository.pushChanges().onFailure { error ->
                Log.e(TAG, "Lỗi khi đẩy thay đổi: ${error.message}")
                // Không làm hỏng worker ngay, vẫn thử kéo các thay đổi
            }

            // Sau đó kéo các thay đổi từ server
            syncRepository.quickSync().fold(
                onSuccess = {
                    Log.d(TAG, "Đồng bộ hoàn tất thành công")
                    return Result.success()
                },
                onFailure = { error ->
                    Log.e(TAG, "Lỗi khi đồng bộ: ${error.message}")
                    return Result.retry()
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi không mong đợi trong quá trình đồng bộ: ${e.message}")
            return Result.retry()
        }

        return Result.success()
    }
}