package com.example.taskapplication.ui.team.detail.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.taskapplication.ui.components.GradientButton
import com.example.taskapplication.ui.components.GradientCard
import com.example.taskapplication.ui.team.detail.InviteState

/**
 * Dialog hiện đại để mời thành viên vào team
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedInviteMemberDialog(
    inviteState: InviteState,
    onDismiss: () -> Unit,
    onInvite: (email: String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    // Hiệu ứng rung khi có lỗi
    val shakeError = inviteState is InviteState.Error
    val translateX by animateFloatAsState(
        targetValue = if (shakeError) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "Shake Animation"
    )
    val shakeOffset = if (shakeError) (translateX * 10 - 5).dp else 0.dp

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        GradientCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer { translationX = shakeOffset.toPx() },
            cornerRadius = 16.dp,
            elevation = 8.dp,
            gradient = Brush.linearGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surface,
                    MaterialTheme.colorScheme.surfaceVariant
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Text(
                    text = "Invite Team Member",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter the email address of the person you want to invite to this team.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Error message
                AnimatedVisibility(
                    visible = inviteState is InviteState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp)
                    ) {
                        if (inviteState is InviteState.Error) {
                            Text(
                                text = inviteState.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Email input
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = inviteState is InviteState.Error,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Loading indicator
                AnimatedVisibility(
                    visible = inviteState is InviteState.Loading,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Success message
                AnimatedVisibility(
                    visible = inviteState is InviteState.Success,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Success",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Invitation sent successfully!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Cancel button
                    GradientButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                Color.Gray.copy(alpha = 0.5f),
                                Color.Gray.copy(alpha = 0.7f)
                            )
                        ),
                        cornerRadius = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Invite button
                    GradientButton(
                        text = "Invite",
                        onClick = { onInvite(email.trim()) },
                        enabled = email.isNotBlank() && inviteState !is InviteState.Loading,
                        modifier = Modifier.weight(1f),
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ),
                        cornerRadius = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
