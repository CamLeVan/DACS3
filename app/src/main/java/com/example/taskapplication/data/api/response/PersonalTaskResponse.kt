package com.example.taskapplication.data.api.response

data class PersonalTaskResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val due_date: String,
    val created_at: String,
    val updated_at: String,
    val subtasks: List<SubtaskResponse>? = null
)

data class SubtaskResponse(
    val id: String,
    val title: String,
    val is_completed: Boolean
)
