package com.example.taskapplication.data.mapper

import com.example.taskapplication.data.local.entity.TeamRoleHistoryEntity
import com.example.taskapplication.domain.model.TeamRoleHistory

/**
 * Mapper for converting between TeamRoleHistory domain model and TeamRoleHistoryEntity
 */
object TeamRoleHistoryMapper {
    
    /**
     * Convert TeamRoleHistoryEntity to TeamRoleHistory
     */
    fun TeamRoleHistoryEntity.toTeamRoleHistory(): TeamRoleHistory {
        return TeamRoleHistory(
            id = id,
            teamId = teamId,
            userId = userId,
            oldRole = oldRole,
            newRole = newRole,
            changedByUserId = changedByUserId,
            timestamp = timestamp,
            syncStatus = syncStatus
        )
    }
    
    /**
     * Convert TeamRoleHistory to TeamRoleHistoryEntity
     */
    fun TeamRoleHistory.toTeamRoleHistoryEntity(): TeamRoleHistoryEntity {
        return TeamRoleHistoryEntity(
            id = id,
            teamId = teamId,
            userId = userId,
            oldRole = oldRole,
            newRole = newRole,
            changedByUserId = changedByUserId,
            timestamp = timestamp,
            syncStatus = syncStatus
        )
    }
    
    /**
     * Convert list of TeamRoleHistoryEntity to list of TeamRoleHistory
     */
    fun List<TeamRoleHistoryEntity>.toTeamRoleHistoryList(): List<TeamRoleHistory> {
        return map { it.toTeamRoleHistory() }
    }
}
