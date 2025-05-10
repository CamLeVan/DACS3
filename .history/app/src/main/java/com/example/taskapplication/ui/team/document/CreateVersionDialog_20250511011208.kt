package com.example.taskapplication.ui.team.document

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.File
import java.io.FileOutputStream

/**
 * Dialog for creating a new document version
 */
@Composable
fun CreateVersionDialog(
    documentId: String,
    onDismiss: () -> Unit,
    onCreateVersion: (file: File, changeNotes: String) -> Unit
) {
    val context = LocalContext.current
    var changeNotes by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedFileUri = it
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex("_display_name")
                    if (displayNameIndex != -1) {
                        selectedFileName = cursor.getString(displayNameIndex)
                    }
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm phiên bản mới") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = changeNotes,
                    onValueChange = { changeNotes = it },
                    label = { Text("Ghi chú thay đổi") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // File picker button with animation
                var isButtonHovered by remember { mutableStateOf(false) }
                val buttonScale by animateFloatAsState(
                    targetValue = if (isButtonHovered) 1.05f else 1f,
                    animationSpec = tween(150),
                    label = "Button Scale"
                )

                // Pulsating animation for button
                val pulse = rememberInfiniteTransition().animateFloat(
                    initialValue = 0.98f,
                    targetValue = 1.02f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "Button Pulse"
                )

                Button(
                    onClick = { filePicker.launch("*/*") },
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .graphicsLayer {
                            scaleX = buttonScale * pulse.value
                            scaleY = buttonScale * pulse.value
                        }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isButtonHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                                }
                            }
                        }
                ) {
                    // Icon with rotation animation
                    val iconRotation by animateFloatAsState(
                        targetValue = if (isButtonHovered) 15f else 0f,
                        animationSpec = tween(150),
                        label = "Icon Rotation"
                    )

                    Icon(
                        Icons.Default.AttachFile,
                        contentDescription = "Chọn file",
                        modifier = Modifier.graphicsLayer {
                            rotationZ = iconRotation
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Chọn file")
                }

                if (selectedFileName.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Animated file name appearance
                    val fileNameScale = remember { Animatable(0.8f) }
                    LaunchedEffect(selectedFileName) {
                        fileNameScale.snapTo(0.8f)
                        fileNameScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .graphicsLayer {
                                scaleX = fileNameScale.value
                                scaleY = fileNameScale.value
                            }
                    ) {
                        Text(
                            text = "File đã chọn: $selectedFileName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            // Confirm button with animation
            var isConfirmHovered by remember { mutableStateOf(false) }
            val confirmEnabled = selectedFileUri != null

            val confirmScale by animateFloatAsState(
                targetValue = if (isConfirmHovered && confirmEnabled) 1.1f else 1f,
                animationSpec = tween(150),
                label = "Confirm Button Scale"
            )

            // Use simple color selection instead of animation
            val confirmColor = if (confirmEnabled)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

            TextButton(
                onClick = {
                    selectedFileUri?.let { uri ->
                        val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}.${getFileExtension(selectedFileName)}")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        onCreateVersion(tempFile, changeNotes)
                    }
                },
                enabled = confirmEnabled,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = confirmScale
                        scaleY = confirmScale
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                isConfirmHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                            }
                        }
                    }
            ) {
                Text(
                    "Tạo",
                    color = confirmColor
                )
            }
        },
        dismissButton = {
            // Dismiss button with animation
            var isDismissHovered by remember { mutableStateOf(false) }
            val dismissScale by animateFloatAsState(
                targetValue = if (isDismissHovered) 1.1f else 1f,
                animationSpec = tween(150),
                label = "Dismiss Button Scale"
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = dismissScale
                        scaleY = dismissScale
                    }
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                isDismissHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                            }
                        }
                    }
            ) {
                Text(
                    "Hủy",
                    color = if (isDismissHovered)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

/**
 * Get file extension from file name
 */
private fun getFileExtension(fileName: String): String {
    return fileName.substringAfterLast('.', "")
}
