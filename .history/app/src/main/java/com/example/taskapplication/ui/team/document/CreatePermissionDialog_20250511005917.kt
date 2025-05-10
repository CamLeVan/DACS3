package com.example.taskapplication.ui.team.document

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Dialog for creating a new permission
 */
@Composable
fun CreatePermissionDialog(
    documentId: String,
    onDismiss: () -> Unit,
    onCreatePermission: (userId: String, permissionType: String) -> Unit
) {
    var userId by remember { mutableStateOf("") }
    var selectedPermissionType by remember { mutableStateOf("view") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm quyền truy cập") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = userId,
                    onValueChange = { userId = it },
                    label = { Text("ID người dùng") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Loại quyền:")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    PermissionTypeOption(
                        label = "Xem",
                        selected = selectedPermissionType == "view",
                        onClick = { selectedPermissionType = "view" }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PermissionTypeOption(
                        label = "Chỉnh sửa",
                        selected = selectedPermissionType == "edit",
                        onClick = { selectedPermissionType = "edit" }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    PermissionTypeOption(
                        label = "Quản trị",
                        selected = selectedPermissionType == "admin",
                        onClick = { selectedPermissionType = "admin" }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (userId.isNotBlank()) {
                        onCreatePermission(userId, selectedPermissionType)
                    }
                },
                enabled = userId.isNotBlank()
            ) {
                Text("Thêm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/**
 * Permission type option
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionTypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Hover state and animations
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.05f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "Chip Scale Animation"
    )

    // Color animation
    val containerColor by animateColorAsState(
        targetValue = when {
            selected && isHovered -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.primaryContainer
            isHovered -> MaterialTheme.colorScheme.surfaceVariant
            else -> MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(durationMillis = 200),
        label = "Chip Color Animation"
    )

    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                color = if (selected && isHovered)
                    MaterialTheme.colorScheme.onPrimary
                else if (selected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurface
            )
        },
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        isHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                    }
                }
            },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = containerColor
        )
    )
}
