package com.example.taskapplication.domain.model

/**
 * Domain model for task analytics
 */
data class TaskAnalytics(
    val totalTasks: Int,
    val completedTasks: Int,
    val overdueTasks: Int,
    val completionRate: Int,
    val byStatus: Map<String, Int>,
    val byPriority: Map<String, Int>,
    val byAssignee: List<AssigneeAnalytics>,
    val timeline: List<TimelineAnalytics>
)

/**
 * Domain model for assignee analytics
 */
data class AssigneeAnalytics(
    val userId: String,
    val name: String,
    val total: Int,
    val completed: Int
)

/**
 * Domain model for timeline analytics
 */
data class TimelineAnalytics(
    val date: String,
    val created: Int,
    val completed: Int
)
