package com.example.taskapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.taskapplication.data.database.entities.AttachmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO để tương tác với bảng attachments trong cơ sở dữ liệu
 */
@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Update
    suspend fun updateAttachment(attachment: AttachmentEntity)

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachmentById(id: String): AttachmentEntity?

    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    fun getAttachmentsByMessageId(messageId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE messageId = :messageId")
    suspend fun getAttachmentsByMessageIdSync(messageId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE syncStatus = 'pending'")
    suspend fun getPendingAttachments(): List<AttachmentEntity>

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachment(id: String)

    @Query("DELETE FROM attachments WHERE messageId = :messageId")
    suspend fun deleteAttachmentsByMessageId(messageId: String)
}
