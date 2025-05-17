package com.example.taskapplication.data.api.response

data class PersonalTaskResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val due_date: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val reminder_date: String? = null,
    val created_at: String,
    val updated_at: String,
    val subtasks: List<SubtaskResponse>? = null
)

data class SubtaskResponse(
    val id: String,
    val title: String,
    val is_completed: Boolean
)

data class PersonalTaskListResponse(
    val data: List<PersonalTaskResponse>,
    val meta: PaginationMeta? = null
)
