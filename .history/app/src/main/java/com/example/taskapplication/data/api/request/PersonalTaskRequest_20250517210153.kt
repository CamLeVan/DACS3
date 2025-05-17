package com.example.taskapplication.data.api.request

import com.google.gson.annotations.SerializedName

data class PersonalTaskRequest(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("status")
    val status: String,

    @SerializedName("priority")
    val priority: String,

    @SerializedName("due_date")
    val dueDate: String? = null,

    @SerializedName("order")
    val order: Int = 0
)
