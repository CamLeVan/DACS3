package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.UserRequest
import com.example.taskapplication.data.api.response.UserResponse
import com.example.taskapplication.data.database.entities.UserEntity
import com.example.taskapplication.domain.model.User
import java.util.*

// Entity to Domain
fun UserEntity.toDomainModel(): User {
    return User(
        id = id,
        name = name,
        email = email,
        avatar = avatar,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// Domain to Entity
fun User.toEntity(): UserEntity {
    return UserEntity(
        id = id,
        name = name,
        email = email,
        avatar = avatar,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// API Response to Entity
fun UserResponse.toEntity(existingUser: UserEntity? = null): UserEntity {
    return UserEntity(
        id = existingUser?.id ?: UUID.randomUUID().toString(),
        name = name,
        email = email,
        avatar = avatar,
        serverId = id.toString(),
        syncStatus = "synced",
        lastModified = updatedAt,
        createdAt = existingUser?.createdAt ?: createdAt
    )
}

// Entity to API Request
fun UserEntity.toApiRequest(): UserRequest {
    return UserRequest(
        name = name,
        email = email,
        avatar = avatar
    )
}

// Domain to API Request
fun User.toApiRequest(): UserRequest {
    return UserRequest(
        name = name,
        email = email,
        avatar = avatar
    )
}

// API Response to Domain
fun UserResponse.toDomainModel(existingUser: User? = null): User {
    return User(
        id = existingUser?.id ?: UUID.randomUUID().toString(),
        name = name,
        email = email,
        avatar = avatar,
        serverId = id,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis(),
        createdAt = existingUser?.createdAt ?: System.currentTimeMillis()
    )
}