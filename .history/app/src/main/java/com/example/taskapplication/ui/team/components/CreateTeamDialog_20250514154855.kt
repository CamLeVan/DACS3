package com.example.taskapplication.ui.team.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.taskapplication.ui.team.CreateTeamState

/**
 * Dialog for creating a new team
 */
@Composable
fun CreateTeamDialog(
    createTeamState: CreateTeamState,
    onDismiss: () -> Unit,
    onCreateTeam: (name: String, description: String?) -> Unit
) {
    var teamName by remember { mutableStateOf("") }
    var teamDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tạo nhóm mới") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                if (createTeamState is CreateTeamState.Error) {
                    // Shaking animation for error message
                    val shake = rememberInfiniteTransition().animateFloat(
                        initialValue = -3f,
                        targetValue = 3f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(150, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Error Shake"
                    )

                    Text(
                        text = createTeamState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { translationX = shake.value }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Tên nhóm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = createTeamState is CreateTeamState.Error
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = teamDescription,
                    onValueChange = { teamDescription = it },
                    label = { Text("Mô tả (Không bắt buộc)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                if (createTeamState is CreateTeamState.Loading) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Rotating and pulsating animation for loading indicator
                    val rotation = rememberInfiniteTransition().animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "Loading Rotation"
                    )

                    val scale = rememberInfiniteTransition().animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Loading Scale"
                    )

                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(36.dp)
                            .graphicsLayer {
                                rotationZ = rotation.value
                                scaleX = scale.value
                                scaleY = scale.value
                            }
                    )
                }
            }
        },
        confirmButton = {
            // Button with animation
            val buttonEnabled = teamName.isNotBlank() && createTeamState !is CreateTeamState.Loading
            val buttonScale by animateFloatAsState(
                targetValue = if (buttonEnabled) 1f else 0.95f,
                animationSpec = tween(150),
                label = "Button Scale Animation"
            )

            Button(
                onClick = {
                    onCreateTeam(
                        teamName.trim(),
                        if (teamDescription.isBlank()) null else teamDescription.trim()
                    )
                },
                enabled = buttonEnabled,
                modifier = Modifier.graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                }
            ) {
                Text("Tạo nhóm")
            }
        },
        dismissButton = {
            // Cancel button with animation
            val cancelEnabled = createTeamState !is CreateTeamState.Loading
            val cancelScale by animateFloatAsState(
                targetValue = if (cancelEnabled) 1f else 0.95f,
                animationSpec = tween(150),
                label = "Cancel Button Scale Animation"
            )

            TextButton(
                onClick = onDismiss,
                enabled = cancelEnabled,
                modifier = Modifier.graphicsLayer {
                    scaleX = cancelScale
                    scaleY = cancelScale
                    alpha = if (cancelEnabled) 1f else 0.6f
                }
            ) {
                Text("Hủy")
            }
        }
    )
}
