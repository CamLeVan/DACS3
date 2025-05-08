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
        userId = id, // Using id as userId since it's a personal task
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
        dueDate = due_date.toLongOrNull(),
        priority = priority.toIntOrNull() ?: 0,
        isCompleted = status == "completed",
        serverId = id,
        syncStatus = "synced",
        lastModified = updated_at.toLongOrNull() ?: System.currentTimeMillis(),
        createdAt = created_at.toLongOrNull() ?: System.currentTimeMillis()
    )
}

// Entity to API Request
fun PersonalTaskEntity.toApiRequest(): PersonalTaskRequest {
    return PersonalTaskRequest(
        title = title,
        description = description,
        status = if (isCompleted) "completed" else "pending",
        priority = priority.toString(),
        due_date = dueDate?.toString() ?: "",
        subtasks = null
    )
}

// Domain to API Request
fun PersonalTask.toApiRequest(): PersonalTaskRequest {
    return PersonalTaskRequest(
        title = title,
        description = description,
        status = if (isCompleted) "completed" else "pending",
        priority = priority.toString(),
        due_date = dueDate?.toString() ?: "",
        subtasks = null
    )
} 