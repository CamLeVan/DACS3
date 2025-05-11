package com.example.taskapplication.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity để lưu trữ thông tin tương tác với người dùng
 * Được sử dụng để gợi ý người dùng phổ biến
 */
@Entity(
    tableName = "user_interactions",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("user_id")
    ]
)
data class UserInteractionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val user_id: String,
    val interaction_type: String, // "message", "task", "document", "invite", etc.
    val interaction_count: Int = 1,
    val last_interaction_timestamp: Long = System.currentTimeMillis()
)
