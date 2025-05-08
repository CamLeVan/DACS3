package com.example.taskapplication.data.api.request

data class UserRequest(
    val name: String,
    val email: String,
    val avatar: String? = null
)
