package com.example.taskapplication.data.database.dao

import androidx.room.*
import com.example.taskapplication.data.database.entities.CalendarEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for calendar events
 */
@Dao
interface CalendarEventDao {
    /**
     * Get all calendar events
     */
    @Query("SELECT * FROM calendar_events ORDER BY startDate ASC")
    fun getAllEvents(): Flow<List<CalendarEventEntity>>
    
    /**
     * Get calendar events by date range
     */
    @Query("SELECT * FROM calendar_events WHERE startDate >= :startDate AND endDate <= :endDate ORDER BY startDate ASC")
    fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<List<CalendarEventEntity>>
    
    /**
     * Get calendar events by team
     */
    @Query("SELECT * FROM calendar_events WHERE teamId = :teamId ORDER BY startDate ASC")
    fun getEventsByTeam(teamId: String): Flow<List<CalendarEventEntity>>
    
    /**
     * Get calendar events by team and date range
     */
    @Query("SELECT * FROM calendar_events WHERE teamId = :teamId AND startDate >= :startDate AND endDate <= :endDate ORDER BY startDate ASC")
    fun getEventsByTeamAndDateRange(teamId: String, startDate: Long, endDate: Long): Flow<List<CalendarEventEntity>>
    
    /**
     * Get calendar event by ID
     */
    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getEventById(id: String): CalendarEventEntity?
    
    /**
     * Insert a new calendar event
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: CalendarEventEntity)
    
    /**
     * Update a calendar event
     */
    @Update
    suspend fun updateEvent(event: CalendarEventEntity)
    
    /**
     * Delete a calendar event
     */
    @Delete
    suspend fun deleteEvent(event: CalendarEventEntity)
    
    /**
     * Get all pending sync events
     */
    @Query("SELECT * FROM calendar_events WHERE syncStatus IN ('pending_create', 'pending_update', 'pending_delete')")
    suspend fun getPendingSyncEvents(): List<CalendarEventEntity>
    
    /**
     * Get event by server ID
     */
    @Query("SELECT * FROM calendar_events WHERE serverId = :serverId")
    suspend fun getEventByServerId(serverId: String): CalendarEventEntity?
}
