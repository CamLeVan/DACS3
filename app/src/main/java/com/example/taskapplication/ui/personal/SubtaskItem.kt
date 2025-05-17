package com.example.taskapplication.ui.personal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskapplication.domain.model.Subtask
import com.example.taskapplication.ui.theme.LocalExtendedColorScheme

/**
 * Component hiển thị một công việc con
 * @param subtask Công việc con cần hiển thị
 * @param onToggleCompletion Callback khi nhấn vào checkbox hoàn thành
 * @param onDelete Callback khi nhấn vào nút xóa
 */
@Composable
fun SubtaskItem(
    subtask: Subtask,
    onToggleCompletion: () -> Unit,
    onDelete: () -> Unit
) {
    val extendedColorScheme = LocalExtendedColorScheme.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Checkbox hiện đại hơn với icon
        IconButton(
            onClick = { onToggleCompletion() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (subtask.completed)
                    Icons.Rounded.CheckCircle
                else
                    Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (subtask.completed)
                    extendedColorScheme.taskCompleted
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (subtask.completed)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface,
            textDecoration = if (subtask.completed) 
                TextDecoration.LineThrough 
            else 
                TextDecoration.None,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(
            onClick = { onDelete() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
