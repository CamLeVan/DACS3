package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.TaskAnalytics
import com.example.taskapplication.domain.model.TeamPerformance

/**
 * Repository interface for analytics
 */
interface AnalyticsRepository {
    /**
     * Get task analytics
     * @param teamId Optional team ID to filter by
     * @param startDate Start date for analytics (YYYY-MM-DD)
     * @param endDate End date for analytics (YYYY-MM-DD)
     * @return Result containing task analytics or an error
     */
    suspend fun getTaskAnalytics(
        teamId: String? = null,
        startDate: String,
        endDate: String
    ): Result<TaskAnalytics>
    
    /**
     * Get team performance
     * @param teamId Team ID
     * @param period Period for analytics (week, month, quarter, year)
     * @param startDate Start date for analytics (YYYY-MM-DD)
     * @return Result containing team performance or an error
     */
    suspend fun getTeamPerformance(
        teamId: String,
        period: String,
        startDate: String
    ): Result<TeamPerformance>
}
