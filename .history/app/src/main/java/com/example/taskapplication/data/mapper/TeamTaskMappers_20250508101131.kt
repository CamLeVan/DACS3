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
        teamId = teamId,
        title = title,
        description = description,
        assignedUserId = assignedUserId,
        dueDate = dueDate,
        priority = priority,
        isCompleted = isCompleted,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// Domain to Entity
fun TeamTask.toEntity(): TeamTaskEntity {
    return TeamTaskEntity(
        id = id,
        teamId = teamId,
        title = title,
        description = description,
        assignedUserId = assignedUserId,
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
fun TeamTaskResponse.toEntity(existingTask: TeamTaskEntity? = null): TeamTaskEntity {
    return TeamTaskEntity(
        id = existingTask?.id ?: UUID.randomUUID().toString(),
        teamId = team_id.toString(),
        title = title,
        description = description,
        assignedUserId = assigned_to.firstOrNull()?.id?.toString(),
        dueDate = due_date,
        priority = priority,
        isCompleted = status == "completed",
        serverId = id.toString(),
        syncStatus = "synced",
        lastModified = updated_at,
        createdAt = existingTask?.createdAt ?: created_at
    )
}

// Entity to API Request
fun TeamTaskEntity.toApiRequest(): TeamTaskRequest {
    return TeamTaskRequest(
        title = title,
        description = description,
        assigned_to = assignedUserId?.toLongOrNull()?.let { listOf(it) } ?: emptyList(),
        due_date = dueDate,
        priority = priority,
        status = if (isCompleted) "completed" else "pending"
    )
}

// Domain to API Request
fun TeamTask.toApiRequest(): TeamTaskRequest {
    return TeamTaskRequest(
        title = title,
        description = description,
        assigned_to = assignedUserId?.toLongOrNull()?.let { listOf(it) } ?: emptyList(),
        due_date = dueDate,
        priority = priority,
        status = if (isCompleted) "completed" else "pending"
    )
} 