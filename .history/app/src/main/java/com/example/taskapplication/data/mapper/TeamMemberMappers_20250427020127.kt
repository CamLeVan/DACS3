package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.api.response.TeamMemberResponse
import com.example.taskapplication.data.database.entities.TeamMemberEntity
import com.example.taskapplication.domain.model.TeamMember
import java.util.*

// Entity to Domain
fun TeamMemberEntity.toDomainModel(): TeamMember {
    return TeamMember(
        id = id,
        teamId = teamId,
        userId = userId,
        role = role,
        joinedAt = joinedAt,
        invitedBy = invitedBy,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// Domain to Entity
fun TeamMember.toEntity(): TeamMemberEntity {
    return TeamMemberEntity(
        id = id,
        teamId = teamId,
        userId = userId,
        role = role,
        joinedAt = joinedAt,
        invitedBy = invitedBy,
        serverId = serverId,
        syncStatus = syncStatus,
        lastModified = lastModified,
        createdAt = createdAt
    )
}

// API Response to Entity
fun TeamMemberResponse.toEntity(existingMember: TeamMemberEntity? = null): TeamMemberEntity {
    return TeamMemberEntity(
        id = existingMember?.id ?: UUID.randomUUID().toString(),
        teamId = teamId.toString(),
        userId = user.id.toString(),
        role = role,
        joinedAt = joinedAt,
        invitedBy = existingMember?.invitedBy,
        serverId = id,
        syncStatus = "synced",
        lastModified = System.currentTimeMillis(),
        createdAt = existingMember?.createdAt ?: joinedAt
    )
}
