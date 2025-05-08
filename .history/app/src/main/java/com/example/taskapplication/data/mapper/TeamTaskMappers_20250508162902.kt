package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.TeamTaskRequest
import com.example.taskapplication.data.api.response.TeamTaskResponse
import com.example.taskapplication.data.database.entities.TeamTaskEntity
import com.example.taskapplication.domain.model.TeamTask
import java.util.*

// Entity to Domain
fun TeamTaskEntity.toDomainModel(): TeamTask {
    return TeamTask(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        teamId = teamId.toString(),
        assignedUserId = assignedUserId?.toString(),
        serverId = serverId?.toString(),
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// Domain to Entity
fun TeamTask.toEntity(): TeamTaskEntity {
    return TeamTaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        teamId = teamId,
        assignedUserId = assignedUserId,
        serverId = serverId?.toString(),
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// API Response to Entity
fun TeamTaskResponse.toEntity(existingTask: TeamTaskEntity? = null): TeamTaskEntity {
    return TeamTaskEntity(
        id = existingTask?.id ?: UUID.randomUUID().toString(),
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        teamId = teamId,
        assignedUserId = assignedUserId,
        serverId = id,
        syncStatus = "synced",
        lastModified = lastModified,
        createdAt = existingTask?.createdAt ?: createdAt
    )
}

// Entity to API Request
fun TeamTaskEntity.toApiRequest(): TeamTaskRequest {
    return TeamTaskRequest(
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        assignedUserId = assignedUserId
    )
}

// Domain to API Request
fun TeamTask.toApiRequest(): TeamTaskRequest {
    return TeamTaskRequest(
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        assignedUserId = assignedUserId
    )
}