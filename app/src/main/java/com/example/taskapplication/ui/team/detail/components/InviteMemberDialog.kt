package com.example.taskapplication.ui.team.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.taskapplication.ui.team.detail.InviteState

/**
 * Dialog for inviting a user to the team
 */
@Composable
fun InviteMemberDialog(
    inviteState: InviteState,
    onDismiss: () -> Unit,
    onInvite: (email: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Invite Team Member") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                if (inviteState is InviteState.Error) {
                    Text(
                        text = inviteState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = inviteState is InviteState.Error
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Enter the email address of the person you want to invite to this team.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (inviteState is InviteState.Loading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onInvite(email.trim()) },
                enabled = email.isNotBlank() && inviteState !is InviteState.Loading
            ) {
                Text("Invite")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = inviteState !is InviteState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}
