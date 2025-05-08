package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.domain.model.TaskAnalytics
import com.example.taskapplication.domain.model.TeamPerformance
import com.example.taskapplication.domain.repository.AnalyticsRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val connectionChecker: ConnectionChecker
) : AnalyticsRepository {

    private val TAG = "AnalyticsRepository"

    override suspend fun getTaskAnalytics(
        teamId: String?,
        startDate: String,
        endDate: String
    ): Result<TaskAnalytics> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val response = apiService.getTaskAnalytics(
                teamId = teamId?.toLongOrNull(),
                startDate = startDate,
                endDate = endDate
            )
            
            if (response.isSuccessful && response.body() != null) {
                val taskAnalytics = response.body()!!.toDomainModel()
                return Result.success(taskAnalytics)
            } else {
                return Result.failure(IOException("Failed to get task analytics: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting task analytics", e)
            return Result.failure(e)
        }
    }

    override suspend fun getTeamPerformance(
        teamId: String,
        period: String,
        startDate: String
    ): Result<TeamPerformance> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            val response = apiService.getTeamPerformance(
                teamId = teamId.toLong(),
                period = period,
                startDate = startDate
            )
            
            if (response.isSuccessful && response.body() != null) {
                val teamPerformance = response.body()!!.toDomainModel()
                return Result.success(teamPerformance)
            } else {
                return Result.failure(IOException("Failed to get team performance: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting team performance", e)
            return Result.failure(e)
        }
    }
}
