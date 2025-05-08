package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.response.UserResponse
import com.example.taskapplication.domain.model.User
import java.util.UUID

// API Response to Domain
fun UserResponse.toDomainModel(): User {
    return User(
        id = id.toString(),
        name = name,
        email = email,
        avatar = avatar,
        serverId = id,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis()
    )
}
