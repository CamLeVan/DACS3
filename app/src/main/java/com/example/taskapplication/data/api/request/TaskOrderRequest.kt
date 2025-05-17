package com.example.taskapplication.data.api.request

import com.google.gson.annotations.SerializedName

data class TaskOrderRequest(
    @SerializedName("tasks")
    val tasks: List<TaskOrderItem>
)

data class TaskOrderItem(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("order")
    val order: Int
)
