package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.TeamEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    @Query("SELECT * FROM teams")
    fun getAllTeams(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    fun getTeamById(teamId: String): Flow<TeamEntity?>

    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamByIdSync(teamId: String): TeamEntity?

    @Query("SELECT * FROM teams WHERE serverId = :serverId")
    suspend fun getTeamByServerIdSync(serverId: Long): TeamEntity?

    @Query("SELECT t.* FROM teams t JOIN team_members tm ON t.id = tm.teamId WHERE tm.userId = :userId")
    fun getTeamsForUser(userId: String): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams WHERE ownerId = :userId")
    fun getTeamsByOwner(userId: String): Flow<List<TeamEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: TeamEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeams(teams: List<TeamEntity>) // For sync

    @Update
    suspend fun updateTeam(team: TeamEntity)

    @Query("UPDATE teams SET syncStatus = 'pending_delete', lastModified = :timestamp WHERE id = :teamId")
    suspend fun markTeamForDeletion(teamId: String, timestamp: Long)

    @Query("DELETE FROM teams WHERE id = :teamId")
    suspend fun deleteTeam(teamId: String)

    @Query("SELECT * FROM teams WHERE syncStatus != 'synced'")
    suspend fun getPendingSyncTeams(): List<TeamEntity>

    @Query("DELETE FROM teams WHERE id = :teamId AND syncStatus = 'pending_create'")
    suspend fun deleteLocalOnlyTeam(teamId: String)

    @Query("DELETE FROM teams WHERE id = :teamId")
    suspend fun deleteTeam(teamId: String)

    @Query("SELECT * FROM teams WHERE syncStatus != 'synced'")
    suspend fun getPendingSyncTeams(): List<TeamEntity>
}