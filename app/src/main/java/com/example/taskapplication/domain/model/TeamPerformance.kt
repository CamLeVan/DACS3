package com.example.taskapplication.domain.model

/**
 * Domain model for team performance
 */
data class TeamPerformance(
    val teamId: String,
    val teamName: String,
    val period: String,
    val startDate: String,
    val metrics: PerformanceMetrics,
    val memberPerformance: List<MemberPerformance>,
    val trends: PerformanceTrends
)

/**
 * Domain model for performance metrics
 */
data class PerformanceMetrics(
    val taskCompletionRate: Int,
    val averageCompletionTime: String,
    val onTimeDeliveryRate: Int,
    val teamVelocity: Int
)

/**
 * Domain model for member performance
 */
data class MemberPerformance(
    val userId: String,
    val name: String,
    val tasksCompleted: Int,
    val onTimeRate: Int,
    val contributionScore: Int
)

/**
 * Domain model for performance trends
 */
data class PerformanceTrends(
    val completionRate: List<TrendPoint>,
    val velocity: List<TrendPoint>
)

/**
 * Domain model for trend point
 */
data class TrendPoint(
    val date: String,
    val value: Int
)
