package com.example.taskapplication.data.api.response

// This file is kept for reference but the actual implementations are in
// TaskAnalyticsResponse.kt and TeamPerformanceResponse.kt

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