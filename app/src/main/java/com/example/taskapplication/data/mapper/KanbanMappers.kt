package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.MoveTaskRequest
import com.example.taskapplication.data.api.response.*
import com.example.taskapplication.data.database.entities.*
import com.example.taskapplication.domain.model.*
import java.text.SimpleDateFormat
import java.util.*

private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

/**
 * Convert KanbanResponse to KanbanBoard domain model
 */
fun KanbanResponse.toDomainModel(): KanbanBoard {
    return KanbanBoard(
        id = UUID.randomUUID().toString(),
        name = name,
        teamId = "", // This would be set by the caller
        columns = columns.map { it.toDomainModel() },
        serverId = id,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis()
    )
}

/**
 * Convert KanbanColumnResponse to KanbanColumn domain model
 */
fun KanbanColumnResponse.toDomainModel(): KanbanColumn {
    return KanbanColumn(
        id = UUID.randomUUID().toString(),
        name = name,
        order = order,
        tasks = tasks.map { it.toDomainModel() },
        serverId = id,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis()
    )
}

/**
 * Convert KanbanTaskResponse to KanbanTask domain model
 */
fun KanbanTaskResponse.toDomainModel(): KanbanTask {
    return KanbanTask(
        id = UUID.randomUUID().toString(),
        title = title,
        description = description,
        priority = priority,
        dueDate = if (due_date != null) parseDate(due_date) else null,
        assignedTo = assigned_to?.toDomainModel(),
        position = position,
        serverId = id,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis()
    )
}

/**
 * Convert KanbanUserResponse to KanbanUser domain model
 */
fun KanbanUserResponse.toDomainModel(): KanbanUser {
    return KanbanUser(
        id = id,
        name = name,
        avatar = avatar
    )
}

/**
 * Convert KanbanBoardEntity and related entities to KanbanBoard domain model
 */
fun KanbanBoardEntity.toDomainModel(columns: List<KanbanColumn>): KanbanBoard {
    return KanbanBoard(
        id = id,
        name = name,
        teamId = teamId,
        columns = columns,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert KanbanColumnEntity and related entities to KanbanColumn domain model
 */
fun KanbanColumnEntity.toDomainModel(tasks: List<KanbanTask>): KanbanColumn {
    return KanbanColumn(
        id = id,
        name = name,
        order = order,
        tasks = tasks,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert KanbanTaskEntity to KanbanTask domain model
 */
fun KanbanTaskEntity.toDomainModel(): KanbanTask {
    return KanbanTask(
        id = id,
        title = title,
        description = description,
        priority = priority,
        dueDate = dueDate,
        assignedTo = if (assignedToId != null) {
            KanbanUser(
                id = assignedToId,
                name = assignedToName ?: "",
                avatar = assignedToAvatar
            )
        } else null,
        position = position,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert KanbanBoard domain model to KanbanBoardEntity
 */
fun KanbanBoard.toEntity(): KanbanBoardEntity {
    return KanbanBoardEntity(
        id = id,
        name = name,
        teamId = teamId,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert KanbanColumn domain model to KanbanColumnEntity
 */
fun KanbanColumn.toEntity(boardId: String): KanbanColumnEntity {
    return KanbanColumnEntity(
        id = id,
        name = name,
        boardId = boardId,
        order = order,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert KanbanTask domain model to KanbanTaskEntity
 */
fun KanbanTask.toEntity(columnId: String): KanbanTaskEntity {
    return KanbanTaskEntity(
        id = id,
        title = title,
        description = description,
        columnId = columnId,
        priority = priority,
        dueDate = dueDate,
        assignedToId = assignedTo?.id,
        assignedToName = assignedTo?.name,
        assignedToAvatar = assignedTo?.avatar,
        position = position,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified
    )
}

/**
 * Convert KanbanTask domain model to MoveTaskRequest
 */
fun KanbanTask.toMoveRequest(columnId: String): MoveTaskRequest {
    return MoveTaskRequest(
        column_id = columnId,
        position = position
    )
}

/**
 * Parse date string to timestamp
 */
private fun parseDate(dateString: String): Long {
    return try {
        dateFormat.parse(dateString)?.time ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}
