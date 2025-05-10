package com.example.taskapplication.ui.team.document

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
