package com.example.taskapplication.data.api.response

/**
 * Response model for team performance
 */
data class TeamPerformanceResponse(
    val team_id: String,
    val team_name: String,
    val period: String,
    val start_date: String,
    val metrics: PerformanceMetricsResponse,
    val member_performance: List<MemberPerformanceResponse>,
    val trends: PerformanceTrendsResponse
)

/**
 * Response model for performance metrics
 */
data class PerformanceMetricsResponse(
    val task_completion_rate: Int,
    val average_completion_time: String,
    val on_time_delivery_rate: Int,
    val team_velocity: Int
)

/**
 * Response model for member performance
 */
data class MemberPerformanceResponse(
    val user_id: String,
    val name: String,
    val tasks_completed: Int,
    val on_time_rate: Int,
    val contribution_score: Int
)

/**
 * Response model for performance trends
 */
data class PerformanceTrendsResponse(
    val completion_rate: List<TrendPointResponse>,
    val velocity: List<TrendPointResponse>
)

/**
 * Response model for trend point
 */
data class TrendPointResponse(
    val date: String,
    val value: Int
)
