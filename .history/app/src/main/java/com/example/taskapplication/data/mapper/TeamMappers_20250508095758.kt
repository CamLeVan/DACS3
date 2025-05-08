package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.request.TeamRequest
import com.example.taskapplication.data.api.response.TeamResponse
import com.example.taskapplication.data.database.entities.TeamEntity
import com.example.taskapplication.domain.model.Team
import java.util.*

// Entity to Domain
fun TeamEntity.toDomainModel(): Team {
    return Team(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        createdBy = createdBy,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// Domain to Entity
fun Team.toEntity(): TeamEntity {
    return TeamEntity(
        id = id,
        name = name,
        description = description,
        ownerId = ownerId,
        createdBy = createdBy,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// API Response to Entity
fun TeamResponse.toEntity(existingTeam: TeamEntity? = null): TeamEntity {
    return TeamEntity(
        id = existingTeam?.id ?: UUID.randomUUID().toString(),
        name = name,
        description = description,
        ownerId = owner.id.toString(),
        createdBy = owner.id.toString(),
        serverId = id.toString(),
        syncStatus = "synced",
        lastModified = updated_at.toLong(),
        createdAt = existingTeam?.createdAt ?: created_at.toLong()
    )
}

// Entity to API Request
fun TeamEntity.toApiRequest(): TeamRequest {
    return TeamRequest(
        name = name,
        description = description
    )
}

// Domain to API Request
fun Team.toApiRequest(): TeamRequest {
    return TeamRequest(
        name = name,
        description = description
    )
}