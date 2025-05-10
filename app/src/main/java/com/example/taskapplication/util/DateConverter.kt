package com.example.taskapplication.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for converting between Date and Long
 */
object DateConverter {
    /**
     * Convert Date to Long (milliseconds since epoch)
     */
    fun dateToLong(date: Date): Long {
        return date.time
    }
    
    /**
     * Convert Long (milliseconds since epoch) to Date
     */
    fun longToDate(time: Long): Date {
        return Date(time)
    }
    
    /**
     * Format Date to string
     */
    fun formatDate(date: Date, pattern: String = "dd/MM/yyyy HH:mm"): String {
        val formatter = SimpleDateFormat(pattern, Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Format Long (milliseconds since epoch) to string
     */
    fun formatLong(time: Long, pattern: String = "dd/MM/yyyy HH:mm"): String {
        return formatDate(longToDate(time), pattern)
    }
}
