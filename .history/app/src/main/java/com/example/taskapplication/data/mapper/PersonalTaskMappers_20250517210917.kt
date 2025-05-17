package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.PersonalTaskRequest
import com.example.taskapplication.data.api.response.PersonalTaskResponse
import com.example.taskapplication.data.database.entities.PersonalTaskEntity
import com.example.taskapplication.data.database.entities.SubtaskEntity
import com.example.taskapplication.domain.model.PersonalTask
import com.example.taskapplication.domain.model.Subtask
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*

// Entity to Domain
fun PersonalTaskEntity.toDomainModel(subtasks: List<Subtask>? = null): PersonalTask {
    return PersonalTask(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        priority = priority,
        status = status,
        order = order,
        userId = userId,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        updatedAt = updatedAt,
        subtasks = subtasks
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
        order = order,
        userId = userId,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// API Response to Entity
fun PersonalTaskResponse.toEntity(existingTask: PersonalTaskEntity? = null): PersonalTaskEntity {
    val dueDateLong = dueDate?.let {
        try {
            val temporal: TemporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(it)
            Instant.from(temporal).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    val createdAtLong = try {
        val temporal: TemporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(createdAt)
        Instant.from(temporal).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    val updatedAtLong = try {
        val temporal: TemporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(updatedAt)
        Instant.from(temporal).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    return PersonalTaskEntity(
        id = existingTask?.id ?: UUID.randomUUID().toString(),
        title = title,
        description = description,
        dueDate = dueDateLong,
        priority = priority,
        status = status,
        order = order,
        userId = userId ?: "",
        serverId = id,
        syncStatus = "synced",
        lastModified = updatedAtLong,
        createdAt = existingTask?.createdAt ?: createdAtLong,
        updatedAt = updatedAtLong
    )
}

// Entity to API Request
fun PersonalTaskEntity.toApiRequest(): PersonalTaskRequest {
    return PersonalTaskRequest(
        title = title,
        description = description,
        status = status,
        priority = priority,
        due_date = dueDate?.let { Instant.ofEpochMilli(it).toString() },
        order = order
    )
}

// Subtask mappers
fun SubtaskEntity.toDomainModel(): Subtask {
    return Subtask(
        id = id,
        taskableType = taskableType,
        taskableId = taskableId,
        title = title,
        completed = completed,
        order = order,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun Subtask.toEntity(): SubtaskEntity {
    return SubtaskEntity(
        id = id,
        taskableType = taskableType,
        taskableId = taskableId,
        title = title,
        completed = completed,
        order = order,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun SubtaskResponse.toEntity(existingSubtask: SubtaskEntity? = null): SubtaskEntity {
    val createdAtLong = try {
        Instant.parse(created_at).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    val updatedAtLong = try {
        Instant.parse(updated_at).toEpochMilli()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }

    return SubtaskEntity(
        id = existingSubtask?.id ?: UUID.randomUUID().toString(),
        taskableType = taskable_type,
        taskableId = taskable_id,
        title = title,
        completed = completed,
        order = order,
        serverId = id,
        syncStatus = PersonalTask.SyncStatus.SYNCED,
        lastModified = updatedAtLong,
        createdAt = existingSubtask?.createdAt ?: createdAtLong,
        updatedAt = updatedAtLong
    )
}

fun SubtaskEntity.toApiRequest(): SubtaskRequest {
    return SubtaskRequest(
        title = title,
        is_completed = completed
    )
}