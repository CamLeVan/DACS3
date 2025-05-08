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
        status = status,
        teamId = teamId.toString(),
        assignedUserId = assignedUserId.toString(),
        createdBy = createdBy.toString(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
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
        status = status,
        teamId = teamId.toLong(),
        assignedUserId = assignedUserId.toLong(),
        createdBy = createdBy.toLong(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

// API Response to Entity
fun TeamTaskResponse.toEntity(existingTask: TeamTaskEntity? = null): TeamTaskEntity {
    return TeamTaskEntity(
        id = existingTask?.id ?: 0L,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        status = status,
        teamId = teamId.toLong(),
        assignedUserId = assignedTo.toLong(),
        createdBy = createdBy.toLong(),
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified ?: System.currentTimeMillis()
    )
}

// Entity to API Request
fun TeamTaskEntity.toApiRequest(): TeamTaskRequest {
    return TeamTaskRequest(
        title = title,
        description = description,
        due_date = dueDate,
        priority = priority,
        status = if (status) "completed" else "pending",
        assigned_to = assignedUserId.toString(),
        created_by = createdBy.toString()
    )
}

// Domain to API Request
fun TeamTask.toApiRequest(): TeamTaskRequest {
    return TeamTaskRequest(
        title = title,
        description = description,
        due_date = dueDate,
        priority = priority,
        status = if (status) "completed" else "pending",
        assigned_to = assignedUserId.toString(),
        created_by = createdBy.toString()
    )
} 