package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.PersonalTaskRequest
import com.example.taskapplication.data.api.response.PersonalTaskResponse
import com.example.taskapplication.data.database.entities.PersonalTaskEntity
import com.example.taskapplication.domain.model.PersonalTask
import java.time.Instant
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
        category = category,
        tags = tags,
        reminderDate = reminderDate,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        updatedAt = updatedAt
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
        category = category,
        tags = tags,
        reminderDate = reminderDate,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// API Response to Entity
fun PersonalTaskResponse.toEntity(existingTask: PersonalTaskEntity? = null): PersonalTaskEntity {
    val dueDateLong = due_date?.let {
        try {
            Instant.parse(it).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    val reminderDateLong = reminder_date?.let {
        try {
            Instant.parse(it).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

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

    return PersonalTaskEntity(
        id = existingTask?.id ?: UUID.randomUUID().toString(),
        title = title,
        description = description,
        dueDate = dueDateLong,
        priority = priority,
        status = status,
        category = category,
        tags = tags,
        reminderDate = reminderDateLong,
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
        category = category,
        tags = tags,
        reminder_date = reminderDate?.let { Instant.ofEpochMilli(it).toString() },
        subtasks = null
    )
}