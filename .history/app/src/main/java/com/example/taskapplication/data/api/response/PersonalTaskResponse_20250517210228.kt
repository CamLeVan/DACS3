package com.example.taskapplication.data.api.response

import com.example.taskapplication.data.database.entities.PersonalTaskEntity
import com.google.gson.annotations.SerializedName
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.UUID

data class PersonalTaskResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String? = null,

    @SerializedName("status")
    val status: String,

    @SerializedName("priority")
    val priority: String,

    @SerializedName("due_date")
    val dueDate: String? = null,

    @SerializedName("order")
    val order: Int = 0,

    @SerializedName("user_id")
    val userId: String? = null,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String
) {
    /**
     * Chuyển đổi từ PersonalTaskResponse sang PersonalTaskEntity
     */
    fun toEntity(existingEntity: PersonalTaskEntity? = null): PersonalTaskEntity {
        val dueDateMillis = dueDate?.let {
            val temporal: TemporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(it)
            Instant.from(temporal).toEpochMilli()
        }

        return PersonalTaskEntity(
            id = existingEntity?.id ?: UUID.randomUUID().toString(),
            serverId = id,
            title = title,
            description = description,
            status = status,
            priority = priority,
            dueDate = dueDateMillis,
            order = order,
            syncStatus = "synced",
            lastModified = System.currentTimeMillis(),
            createdAt = existingEntity?.createdAt ?: System.currentTimeMillis()
        )
    }
}

data class PersonalTaskListResponse(
    @SerializedName("data")
    val data: List<PersonalTaskResponse>,

    @SerializedName("meta")
    val meta: PaginationMeta? = null
)

data class PaginationMeta(
    @SerializedName("current_page")
    val currentPage: Int,

    @SerializedName("last_page")
    val lastPage: Int,

    @SerializedName("per_page")
    val perPage: Int,

    @SerializedName("total")
    val total: Int
)
