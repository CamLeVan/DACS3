package com.example.taskapplication.data.api.response

data class PersonalTaskResponse(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    val priority: String,
    val due_date: String? = null,
    val order: Int = 0,
    val user_id: String? = null,
    val created_at: String,
    val updated_at: String,
    val subtasks: List<SubtaskResponse>? = null
)

data class SubtaskResponse(
    val id: String,
    val taskable_type: String,
    val taskable_id: String,
    val title: String,
    val completed: Boolean,
    val order: Int,
    val created_at: String,
    val updated_at: String
)

data class PersonalTaskListResponse(
    val data: List<PersonalTaskResponse>,
    val meta: PaginationMeta? = null
)

data class PaginationMeta(
    val current_page: Int,
    val last_page: Int,
    val per_page: Int,
    val total: Int
)
