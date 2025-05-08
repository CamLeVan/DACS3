package com.example.taskapplication.data.api.response

/**
 * Response model for task analytics
 */
data class TaskAnalyticsResponse(
    val total_tasks: Int,
    val completed_tasks: Int,
    val overdue_tasks: Int,
    val completion_rate: Int,
    val by_status: Map<String, Int>,
    val by_priority: Map<String, Int>,
    val by_assignee: List<AssigneeAnalyticsResponse>,
    val timeline: List<TimelineAnalyticsResponse>
)

/**
 * Response model for assignee analytics
 */
data class AssigneeAnalyticsResponse(
    val user_id: String,
    val name: String,
    val total: Int,
    val completed: Int
)

/**
 * Response model for timeline analytics
 */
data class TimelineAnalyticsResponse(
    val date: String,
    val created: Int,
    val completed: Int
)
