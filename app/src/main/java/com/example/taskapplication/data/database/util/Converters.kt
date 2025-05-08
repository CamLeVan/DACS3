package com.example.taskapplication.data.database.util

import androidx.room.TypeConverter
import com.example.taskapplication.domain.model.EventParticipant
import com.example.taskapplication.domain.model.KanbanUser
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(",")
    }

    @TypeConverter
    fun toString(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    // Map<String, String> converters
    @TypeConverter
    fun fromStringMap(value: String?): Map<String, String>? {
        if (value == null) return null
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }

    @TypeConverter
    fun toStringMap(map: Map<String, String>?): String? {
        if (map == null) return null
        return gson.toJson(map)
    }

    // EventParticipant list converters
    @TypeConverter
    fun fromEventParticipantList(value: String?): List<EventParticipant>? {
        if (value == null) return null
        val listType = object : TypeToken<List<EventParticipant>>() {}.type
        return gson.fromJson(value, listType)
    }

    @TypeConverter
    fun toEventParticipantList(list: List<EventParticipant>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    // KanbanUser converter
    @TypeConverter
    fun fromKanbanUser(value: String?): KanbanUser? {
        if (value == null) return null
        return gson.fromJson(value, KanbanUser::class.java)
    }

    @TypeConverter
    fun toKanbanUser(user: KanbanUser?): String? {
        if (user == null) return null
        return gson.toJson(user)
    }
}