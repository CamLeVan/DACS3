package com.example.taskapplication.data.api.request

data class PersonalTaskRequest(
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val due_date: String,
    val subtasks: List<SubtaskRequest>? = null
)

data class TeamTaskRequest(
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val due_date: String,
    val assigned_to: List<Long>? = null,
    val subtasks: List<SubtaskRequest>? = null
)

data class SubtaskRequest(
    val title: String
)

data class MoveTaskRequest(
    val column_id: Long,
    val position: Int
) 