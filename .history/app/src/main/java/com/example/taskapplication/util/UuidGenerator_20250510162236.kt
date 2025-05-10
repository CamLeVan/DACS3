package com.example.taskapplication.util

import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for generating UUIDs
 */
@Singleton
class UuidGenerator @Inject constructor() {

    /**
     * Generate a random UUID
     */
    fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }

    companion object {
        /**
         * Generate a random UUID (static method)
         */
        fun generate(): String {
            return UUID.randomUUID().toString()
        }
    }
}
