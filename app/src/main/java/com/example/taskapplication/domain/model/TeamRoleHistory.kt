package com.example.taskapplication.domain.model

import java.util.Date

/**
 * Model class for team role change history
 */
data class TeamRoleHistory(
    val id: Long = 0,
    val teamId: String,
    val userId: String,
    val oldRole: String,
    val newRole: String,
    val changedByUserId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val syncStatus: String = "pending"
) {
    /**
     * Get formatted date from timestamp
     */
    fun getFormattedDate(): String {
        val date = Date(timestamp)
        return date.toString()
    }
    
    /**
     * Get role change description
     */
    fun getChangeDescription(): String {
        return "Changed from $oldRole to $newRole"
    }
}
