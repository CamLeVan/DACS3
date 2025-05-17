package com.example.taskapplication.data.api.request

data class PersonalTaskRequest(
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val due_date: String? = null,
    val order: Int = 0,
    val subtasks: List<SubtaskRequest>? = null
)

data class SubtaskRequest(
    val title: String,
    val is_completed: Boolean
)

data class TaskOrderRequest(
    val tasks: List<TaskOrderItem>
)

data class TaskOrderItem(
    val id: String,
    val order: Int
)

data class SubtaskOrderRequest(
    val subtasks: List<SubtaskOrderItem>
)

data class SubtaskOrderItem(
    val id: String,
    val order: Int
)
