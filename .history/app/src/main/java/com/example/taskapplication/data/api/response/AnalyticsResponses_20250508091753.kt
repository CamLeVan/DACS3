package com.example.taskapplication.data.api.response

data class TaskAnalyticsResponse(
    val total_tasks: Int,
    val completed_tasks: Int,
    val overdue_tasks: Int,
    val completion_rate: Int,
    val by_status: TaskStatusAnalytics,
    val by_priority: TaskPriorityAnalytics,
    val by_assignee: List<AssigneeAnalytics>,
    val timeline: List<TimelineAnalytics>
)

data class TaskStatusAnalytics(
    val todo: Int,
    val in_progress: Int,
    val done: Int
)

data class TaskPriorityAnalytics(
    val low: Int,
    val medium: Int,
    val high: Int
)

data class AssigneeAnalytics(
    val user_id: Long,
    val name: String,
    val total: Int,
    val completed: Int
)

data class TimelineAnalytics(
    val date: String,
    val created: Int,
    val completed: Int
)

data class TeamPerformanceResponse(
    val team_id: Long,
    val team_name: String,
    val period: String,
    val start_date: String,
    val metrics: PerformanceMetrics,
    val member_performance: List<MemberPerformance>,
    val trends: PerformanceTrends
)

data class PerformanceMetrics(
    val task_completion_rate: Int,
    val average_completion_time: String,
    val on_time_delivery_rate: Int,
    val team_velocity: Int
)

data class MemberPerformance(
    val user_id: Long,
    val name: String,
    val tasks_completed: Int,
    val on_time_rate: Int,
    val contribution_score: Int
)

data class PerformanceTrends(
    val completion_rate: List<TrendPoint>,
    val velocity: List<TrendPoint>
)

data class TrendPoint(
    val date: String,
    val value: Int
) 