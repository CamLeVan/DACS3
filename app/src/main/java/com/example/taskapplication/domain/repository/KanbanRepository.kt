package com.example.taskapplication.domain.repository

import com.example.taskapplication.domain.model.KanbanBoard
import com.example.taskapplication.domain.model.KanbanTask
import kotlinx.coroutines.flow.Flow
import java.io.IOException

/**
 * Repository interface for kanban boards
 */
interface KanbanRepository {
    /**
     * Get kanban board by team
     * @param teamId Team ID
     * @return Flow of kanban board
     */
    fun getKanbanBoard(teamId: String): Flow<KanbanBoard?>
    
    /**
     * Move a task to a different column or position
     * @param teamId Team ID
     * @param taskId Task ID
     * @param columnId Column ID to move to
     * @param position Position in the column
     * @return Result containing the moved task or an error
     */
    suspend fun moveTask(
        teamId: String,
        taskId: String,
        columnId: String,
        position: Int
    ): Result<KanbanTask>
    
    /**
     * Sync kanban board with the server
     * @param teamId Team ID
     * @return Result containing success or an error
     */
    suspend fun syncKanbanBoard(teamId: String): Result<Unit>
}
