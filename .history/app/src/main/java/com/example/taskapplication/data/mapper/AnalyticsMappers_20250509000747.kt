package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.response.*
import com.example.taskapplication.domain.model.*

/**
 * Convert TaskAnalyticsResponse to TaskAnalytics domain model
 */
fun TaskAnalyticsResponse.toDomainModel(): TaskAnalytics {
    return TaskAnalytics(
        totalTasks = total_tasks,
        completedTasks = completed_tasks,
        overdueTasks = overdue_tasks,
        completionRate = completion_rate,
        byStatus = by_status,
        byPriority = by_priority,
        byAssignee = by_assignee.map { it.toDomainModel() },
        timeline = timeline.map { it.toDomainModel() }
    )
}

/**
 * Convert AssigneeAnalyticsResponse to AssigneeAnalytics domain model
 */
fun AssigneeAnalyticsResponse.toDomainModel(): AssigneeAnalytics {
    return AssigneeAnalytics(
        userId = user_id,
        name = name,
        total = total,
        completed = completed
    )
}

/**
 * Convert TimelineAnalyticsResponse to TimelineAnalytics domain model
 */
fun TimelineAnalyticsResponse.toDomainModel(): TimelineAnalytics {
    return TimelineAnalytics(
        date = date,
        created = created,
        completed = completed
    )
}

/**
 * Convert TeamPerformanceResponse to TeamPerformance domain model
 */
fun TeamPerformanceResponse.toDomainModel(): TeamPerformance {
    return TeamPerformance(
        teamId = team_id,
        teamName = team_name,
        period = period,
        startDate = start_date,
        metrics = metrics.toDomainModel(),
        memberPerformance = member_performance.map { it.toDomainModel() },
        trends = trends.toDomainModel()
    )
}

/**
 * Convert PerformanceMetricsResponse to PerformanceMetrics domain model
 */
fun PerformanceMetricsResponse.toDomainModel(): PerformanceMetrics {
    return PerformanceMetrics(
        taskCompletionRate = task_completion_rate,
        averageCompletionTime = average_completion_time,
        onTimeDeliveryRate = on_time_delivery_rate,
        teamVelocity = team_velocity
    )
}

/**
 * Convert MemberPerformanceResponse to MemberPerformance domain model
 */
fun MemberPerformanceResponse.toDomainModel(): MemberPerformance {
    return MemberPerformance(
        userId = user_id,
        name = name,
        tasksCompleted = tasks_completed,
        onTimeRate = on_time_rate,
        contributionScore = contribution_score
    )
}

/**
 * Convert PerformanceTrendsResponse to PerformanceTrends domain model
 */
fun PerformanceTrendsResponse.toDomainModel(): PerformanceTrends {
    return PerformanceTrends(
        completionRate = completion_rate.map { it.toDomainModel() },
        velocity = velocity.map { it.toDomainModel() }
    )
}

/**
 * Convert TrendPointResponse to TrendPoint domain model
 */
fun TrendPointResponse.toDomainModel(): TrendPoint {
    return TrendPoint(
        date = date,
        value = value
    )
}
