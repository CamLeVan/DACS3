package com.example.taskapplication.data.repository

import android.util.Log
import com.example.taskapplication.data.api.ApiService
import com.example.taskapplication.data.api.request.MoveTaskRequest
import com.example.taskapplication.data.database.dao.KanbanBoardDao
import com.example.taskapplication.data.database.dao.KanbanColumnDao
import com.example.taskapplication.data.database.dao.KanbanTaskDao
import com.example.taskapplication.data.mapper.*
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.domain.model.KanbanBoard
import com.example.taskapplication.domain.model.KanbanColumn
import com.example.taskapplication.domain.model.KanbanTask
import com.example.taskapplication.domain.repository.KanbanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KanbanRepositoryImpl @Inject constructor(
    private val kanbanBoardDao: KanbanBoardDao,
    private val kanbanColumnDao: KanbanColumnDao,
    private val kanbanTaskDao: KanbanTaskDao,
    private val apiService: ApiService,
    private val connectionChecker: ConnectionChecker
) : KanbanRepository {

    private val TAG = "KanbanRepository"

    override fun getKanbanBoard(teamId: String): Flow<KanbanBoard?> {
        return kanbanBoardDao.getBoardsByTeam(teamId)
            .flatMapLatest { boards ->
                if (boards.isEmpty()) {
                    flowOf(null)
                } else {
                    val board = boards.first()
                    kanbanColumnDao.getColumnsByBoard(board.id)
                        .flatMapLatest { columns ->
                            val columnFlows = columns.map { column ->
                                kanbanTaskDao.getTasksByColumn(column.id)
                                    .map { tasks ->
                                        column.toDomainModel(tasks.map { it.toDomainModel() })
                                    }
                            }
                            
                            if (columnFlows.isEmpty()) {
                                flowOf(board.toDomainModel(emptyList()))
                            } else {
                                combine(columnFlows) { columnArray ->
                                    board.toDomainModel(columnArray.toList())
                                }
                            }
                        }
                }
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun moveTask(
        teamId: String,
        taskId: String,
        columnId: String,
        position: Int
    ): Result<KanbanTask> {
        try {
            // Get the task
            val task = kanbanTaskDao.getTaskById(taskId) ?: return Result.failure(IOException("Task not found"))
            
            // Get the current column tasks
            val currentColumnTasks = kanbanTaskDao.getTasksByColumn(task.columnId).first()
                .filter { it.id != taskId }
                .sortedBy { it.position }
            
            // Get the target column tasks
            val targetColumnTasks = if (task.columnId == columnId) {
                currentColumnTasks
            } else {
                kanbanTaskDao.getTasksByColumn(columnId).first()
                    .sortedBy { it.position }
            }
            
            // Update the task
            val updatedTask = task.copy(
                columnId = columnId,
                position = position,
                syncStatus = "pending_update",
                lastModified = System.currentTimeMillis()
            )
            
            // Update positions of other tasks in the target column
            val updatedTargetTasks = targetColumnTasks.mapIndexed { index, columnTask ->
                if (index < position) {
                    columnTask
                } else {
                    columnTask.copy(
                        position = index + 1,
                        syncStatus = "pending_update",
                        lastModified = System.currentTimeMillis()
                    )
                }
            }
            
            // Save changes to database
            kanbanTaskDao.updateTask(updatedTask)
            kanbanTaskDao.updateTaskPositions(updatedTargetTasks)
            
            // If online, sync with server
            if (connectionChecker.isNetworkAvailable()) {
                try {
                    val request = MoveTaskRequest(columnId, position)
                    val response = apiService.moveKanbanTask(teamId, taskId, request)
                    
                    if (response.isSuccessful && response.body() != null) {
                        val serverTask = response.body()!!
                        
                        // Update task with server data
                        val finalTask = updatedTask.copy(
                            syncStatus = "synced",
                            lastModified = System.currentTimeMillis()
                        )
                        kanbanTaskDao.updateTask(finalTask)
                        
                        return Result.success(finalTask.toDomainModel())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error syncing task move to server", e)
                    // Continue with local update
                }
            }
            
            return Result.success(updatedTask.toDomainModel())
        } catch (e: Exception) {
            Log.e(TAG, "Error moving task", e)
            return Result.failure(e)
        }
    }

    override suspend fun syncKanbanBoard(teamId: String): Result<Unit> {
        if (!connectionChecker.isNetworkAvailable()) {
            return Result.failure(IOException("No network connection"))
        }
        
        try {
            // Get kanban board from server
            val response = apiService.getKanbanBoard(teamId)
            
            if (response.isSuccessful && response.body() != null) {
                val serverBoard = response.body()!!
                
                // Check if board exists locally
                val localBoards = kanbanBoardDao.getBoardsByTeam(teamId).first()
                val localBoard = if (localBoards.isNotEmpty()) localBoards.first() else null
                
                if (localBoard == null) {
                    // Create new board
                    val board = serverBoard.toDomainModel().copy(teamId = teamId)
                    kanbanBoardDao.insertBoard(board.toEntity())
                    
                    // Create columns
                    for (column in board.columns) {
                        kanbanColumnDao.insertColumn(column.toEntity(board.id))
                        
                        // Create tasks
                        for (task in column.tasks) {
                            kanbanTaskDao.insertTask(task.toEntity(column.id))
                        }
                    }
                } else {
                    // Update existing board
                    val updatedBoard = localBoard.copy(
                        name = serverBoard.name,
                        serverId = serverBoard.id,
                        syncStatus = "synced",
                        lastModified = System.currentTimeMillis()
                    )
                    kanbanBoardDao.updateBoard(updatedBoard)
                    
                    // Process columns
                    val serverColumns = serverBoard.columns
                    val localColumns = kanbanColumnDao.getColumnsByBoard(localBoard.id).first()
                    
                    // Map server columns to local columns by name
                    val columnMap = localColumns.associateBy { it.name }
                    
                    for (serverColumn in serverColumns) {
                        val localColumn = columnMap[serverColumn.name]
                        
                        if (localColumn == null) {
                            // Create new column
                            val column = serverColumn.toDomainModel()
                            kanbanColumnDao.insertColumn(column.toEntity(localBoard.id))
                            
                            // Create tasks
                            for (task in column.tasks) {
                                kanbanTaskDao.insertTask(task.toEntity(column.id))
                            }
                        } else {
                            // Update existing column
                            val updatedColumn = localColumn.copy(
                                name = serverColumn.name,
                                order = serverColumn.order,
                                serverId = serverColumn.id,
                                syncStatus = "synced",
                                lastModified = System.currentTimeMillis()
                            )
                            kanbanColumnDao.updateColumn(updatedColumn)
                            
                            // Process tasks
                            val serverTasks = serverColumn.tasks
                            val localTasks = kanbanTaskDao.getTasksByColumn(localColumn.id).first()
                            
                            // Map server tasks to local tasks by title
                            val taskMap = localTasks.associateBy { it.title }
                            
                            for (serverTask in serverTasks) {
                                val localTask = taskMap[serverTask.title]
                                
                                if (localTask == null) {
                                    // Create new task
                                    val task = serverTask.toDomainModel()
                                    kanbanTaskDao.insertTask(task.toEntity(localColumn.id))
                                } else {
                                    // Update existing task
                                    val updatedTask = localTask.copy(
                                        title = serverTask.title,
                                        description = serverTask.description,
                                        priority = serverTask.priority,
                                        dueDate = if (serverTask.due_date != null) parseDate(serverTask.due_date) else null,
                                        assignedToId = serverTask.assigned_to?.id,
                                        assignedToName = serverTask.assigned_to?.name,
                                        assignedToAvatar = serverTask.assigned_to?.avatar,
                                        position = serverTask.position,
                                        serverId = serverTask.id,
                                        syncStatus = "synced",
                                        lastModified = System.currentTimeMillis()
                                    )
                                    kanbanTaskDao.updateTask(updatedTask)
                                }
                            }
                        }
                    }
                }
                
                return Result.success(Unit)
            } else {
                return Result.failure(IOException("Failed to get kanban board: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing kanban board", e)
            return Result.failure(e)
        }
    }
    
    // Helper function to parse date
    private fun parseDate(dateString: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}
