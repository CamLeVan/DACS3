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
        status = status,
        userId = userId.toString(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
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
        status = status,
        userId = userId.toLong(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

// API Response to Entity
fun PersonalTaskResponse.toEntity(existingTask: PersonalTaskEntity? = null): PersonalTaskEntity {
    return PersonalTaskEntity(
        id = existingTask?.id ?: 0L,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        status = status,
        userId = userId.toLong(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified ?: System.currentTimeMillis()
    )
}

// Entity to API Request
fun PersonalTaskEntity.toApiRequest(): PersonalTaskRequest {
    return PersonalTaskRequest(
        title = title,
        description = description,
        due_date = dueDate,
        priority = priority,
        status = if (status) "completed" else "pending"
    )
}

// Domain to API Request
fun PersonalTask.toApiRequest(): PersonalTaskRequest {
    return PersonalTaskRequest(
        title = title,
        description = description,
        due_date = dueDate,
        priority = priority,
        status = if (status) "completed" else "pending"
    )
} 