package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.database.dao.CalendarEventDao
import com.example.taskapplication.data.mapper.toApiRequest
import com.example.taskapplication.data.mapper.toDomainModel
import com.example.taskapplication.data.mapper.toEntity
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.domain.model.CalendarEvent
import com.example.taskapplication.domain.repository.CalendarRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepositoryImpl @Inject constructor(
    private val calendarEventDao: CalendarEventDao,
    private val apiService: ApiService,
    private val connectionChecker: ConnectionChecker
) : CalendarRepository {

    private val TAG = "CalendarRepository"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun getAllEvents(): Flow<List<CalendarEvent>> {
        return calendarEventDao.getAllEvents()
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getEventsByDateRange(startDate: Long, endDate: Long): Flow<List<CalendarEvent>> {
        return calendarEventDao.getEventsByDateRange(startDate, endDate)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getEventsByTeam(teamId: String): Flow<List<CalendarEvent>> {
        return calendarEventDao.getEventsByTeam(teamId)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getEventsByTeamAndDateRange(
        teamId: String,
        startDate: Long,
        endDate: Long
    ): Flow<List<CalendarEvent>> {
        return calendarEventDao.getEventsByTeamAndDateRange(teamId, startDate, endDate)
            .map { entities -> entities.map { it.toDomainModel() } }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun getEventById(id: String): CalendarEvent? {
        return calendarEventDao.getEventById(id)?.toDomainModel()
    }

    override suspend fun createEvent(event: CalendarEvent): Result<CalendarEvent> {
        try {
            // Create a new event with a UUID if not provided
            val eventWithId = if (event.id.isBlank()) {
                event.copy(id = UUID.randomUUID().toString())
            } else {
                event
            }
            
            // Save to local database
            val eventEntity = eventWithId.toEntity().copy(
                syncStatus = "pending_create",
                lastModified = System.currentTimeMillis()
            )
            calendarEventDao.insertEvent(eventEntity)
            
            // If online, sync with server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val request = eventWithId.toApiRequest()
                    val response = apiService.createCalendarEvent(request)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverEvent = response.body()!!
                        val updatedEventEntity = serverEvent.toEntity(eventEntity)
                        calendarEventDao.updateEvent(updatedEventEntity)
                        
                        return Result.success(updatedEventEntity.toDomainModel())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing event to server", e)
                    // Continue with local event
                }
            }
            
            return Result.success(eventEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error creating event", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateEvent(event: CalendarEvent): Result<CalendarEvent> {
        try {
            // Save to local database
            val eventEntity = event.toEntity().copy(
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            calendarEventDao.updateEvent(eventEntity)
            
            // If online, sync with server
            if (connectionChecker.isNetworkAvailable() && event.serverId != null) {
                try {
                    val request = event.toApiRequest()
                    // In a real implementation, we would call an API endpoint to update the event
                    // For now, we'll just assume it's successful
                    
                    // Update local database with synced status
                    val updatedEventEntity = eventEntity.copy(
                        syncStatus = "synced",
                        lastModified = System.currentTimeMillis()
                    )
                    calendarEventDao.updateEvent(updatedEventEntity)
                    
                    return Result.success(updatedEventEntity.toDomainModel())
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing event update to server", e)
                    // Continue with local update
                }
            }
            
            return Result.success(eventEntity.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event", e)
            return Result.failure(e)
        }
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        try {
            val event = calendarEventDao.getEventById(eventId)
            if (event != null) {
                if (event.serverId != null) {
                    // If event has been synced with server, mark for deletion
                    val updatedEvent = event.copy(
                        syncStatus = "pending_delete",
                        lastModified = System.currentTimeMillis()
                    )
                    calendarEventDao.updateEvent(updatedEvent)
                    
                    // If online, sync with server
                    if (connectionChecker.isNetworkAvailable()) {
                        try {
                            // In a real implementation, we would call an API endpoint to delete the event
                            // For now, we'll just assume it's successful
                            calendarEventDao.deleteEvent(updatedEvent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing event deletion to server", e)
                            // Continue with local deletion
                        }
                    }
                } else {
                    // If event has not been synced with server, delete it directly
                    calendarEventDao.deleteEvent(event)
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncEvents(): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            // Get pending sync events
            val pendingEvents = calendarEventDao.getPendingSyncEvents()
            
            // Sync pending events to server
            for (event in pendingEvents) {
                when (event.syncStatus) {
                    "pending_create" -> {
                        try {
                            val request = event.toDomainModel().toApiRequest()
                            val response = apiService.createCalendarEvent(request)
                            
                            if (response.isSuccessful && response.body() != null) {
                                val serverEvent = response.body()!!
                                val updatedEvent = serverEvent.toEntity(event)
                                calendarEventDao.updateEvent(updatedEvent)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing event creation to server", e)
                        }
                    }
                    "pending_update" -> {
                        try {
                            val request = event.toDomainModel().toApiRequest()
                            // In a real implementation, we would call an API endpoint to update the event
                            // For now, we'll just assume it's successful
                            
                            val updatedEvent = event.copy(
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                            calendarEventDao.updateEvent(updatedEvent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing event update to server", e)
                        }
                    }
                    "pending_delete" -> {
                        try {
                            // In a real implementation, we would call an API endpoint to delete the event
                            // For now, we'll just assume it's successful
                            calendarEventDao.deleteEvent(event)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error syncing event deletion to server", e)
                        }
                    }
                }
            }
            
            // Get events from server
            val startDate = dateFormat.format(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) // 30 days ago
            val endDate = dateFormat.format(System.currentTimeMillis() + 90L * 24 * 60 * 60 * 1000) // 90 days ahead
            
            val response = apiService.getCalendarEvents(startDate, endDate)
            
            if (response.isSuccessful && response.body() != null) {
                val serverEvents = response.body()!!
                
                for (serverEvent in serverEvents) {
                    val existingEvent = calendarEventDao.getEventByServerId(serverEvent.id)
                    
                    if (existingEvent == null) {
                        // New event from server
                        val newEvent = serverEvent.toEntity()
                        calendarEventDao.insertEvent(newEvent)
                    } else if (existingEvent.syncStatus == "synced") {
                        // Update existing event if it's synced (not pending changes)
                        val updatedEvent = serverEvent.toEntity(existingEvent)
                        calendarEventDao.updateEvent(updatedEvent)
                    }
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing events", e)
            return Result.failure(e)
        }
    }
}
