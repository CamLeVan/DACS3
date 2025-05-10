package com.example.taskapplication.ui.team.document

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Create document dialog
 */
@Composable
fun AccessLevelOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Helper function to format date
 */
fun formatDate(date: Date): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(date)
}

/**
 * Helper function to get document icon based on file type
 */
fun getDocumentIcon(fileType: String): ImageVector = when {
    fileType.contains("pdf") -> Icons.Default.Folder // Replace with PDF icon
    fileType.contains("image") -> Icons.Default.Folder // Replace with Image icon
    fileType.contains("word") || fileType.contains("document") -> Icons.Default.Folder // Replace with Document icon
    fileType.contains("spreadsheet") || fileType.contains("excel") -> Icons.Default.Folder // Replace with Spreadsheet icon
    fileType.contains("presentation") || fileType.contains("powerpoint") -> Icons.Default.Folder // Replace with Presentation icon
    else -> Icons.Default.Folder // Default icon
}

fun getFileExtension(file: File): String {
    return file.name.substringAfterLast('.', "").lowercase()
}

fun getFileType(file: File): String {
    return when (getFileExtension(file)) {
        "pdf" -> "pdf"
        "doc", "docx" -> "word"
        "xls", "xlsx" -> "excel"
        "ppt", "pptx" -> "powerpoint"
        "txt" -> "text"
        "jpg", "jpeg", "png", "gif" -> "image"
        else -> "unknown"
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val format = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
