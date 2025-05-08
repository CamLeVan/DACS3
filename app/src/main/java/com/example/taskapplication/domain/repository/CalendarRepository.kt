package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.CalendarEvent
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Repository interface for calendar events
 */
interface CalendarRepository {
    /**
     * Get all calendar events
     * @return Flow of list of calendar events
     */
    fun getAllEvents(): Flow<List<CalendarEvent>>
    
    /**
     * Get calendar events by date range
     * @param startDate Start date timestamp
     * @param endDate End date timestamp
     * @return Flow of list of calendar events
     */
    fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<List<CalendarEvent>>
    
    /**
     * Get calendar events by team
     * @param teamId Team ID
     * @return Flow of list of calendar events
     */
    fun getEventsByTeam(teamId: String): Flow<List<CalendarEvent>>
    
    /**
     * Get calendar events by team and date range
     * @param teamId Team ID
     * @param startDate Start date timestamp
     * @param endDate End date timestamp
     * @return Flow of list of calendar events
     */
    fun getEventsByTeamAndDateRange(teamId: String, startDate: Long, endDate: Long): Flow<List<CalendarEvent>>
    
    /**
     * Get calendar event by ID
     * @param id Event ID
     * @return Calendar event or null if not found
     */
    suspend fun getEventById(id: String): CalendarEvent?
    
    /**
     * Create a new calendar event
     * @param event Calendar event to create
     * @return Result containing the created event or an error
     */
    suspend fun createEvent(event: CalendarEvent): Result<CalendarEvent>
    
    /**
     * Update a calendar event
     * @param event Calendar event to update
     * @return Result containing the updated event or an error
     */
    suspend fun updateEvent(event: CalendarEvent): Result<CalendarEvent>
    
    /**
     * Delete a calendar event
     * @param eventId Event ID to delete
     * @return Result containing success or an error
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
    
    /**
     * Sync calendar events with the server
     * @return Result containing success or an error
     */
    suspend fun syncEvents(): Result<Unit>
}
