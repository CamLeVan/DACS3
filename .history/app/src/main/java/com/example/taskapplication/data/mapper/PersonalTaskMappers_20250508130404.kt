package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.PersonalTaskRequest
import com.example.taskapplication.data.api.response.PersonalTaskResponse
import com.example.taskapplication.data.database.entities.PersonalTaskEntity
import com.example.taskapplication.domain.model.PersonalTask
import java.util.*

// Entity to Domain
fun PersonalTaskEntity.toDomainModel(): PersonalTask {
    return PersonalTask(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        userId = userId.toString(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// Domain to Entity
fun PersonalTask.toEntity(): PersonalTaskEntity {
    return PersonalTaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        userId = userId.toLong(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// API Response to Entity
fun PersonalTaskResponse.toEntity(existingTask: PersonalTaskEntity? = null): PersonalTaskEntity {
    return PersonalTaskEntity(
        id = existingTask?.id ?: UUID.randomUUID().toString(),
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        userId = userId.toLong(),
        serverId = id.toString(),
        syncStatus = "synced",
        lastModified = lastModified,
        createdAt = existingTask?.createdAt ?: createdAt
    )
}

// Entity to API Request
fun PersonalTaskEntity.toApiRequest(): PersonalTaskRequest {
    return PersonalTaskRequest(
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted
    )
}

// Domain to API Request
fun PersonalTask.toApiRequest(): PersonalTaskRequest {
    return PersonalTaskRequest(
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted
    )
} 