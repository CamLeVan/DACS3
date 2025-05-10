package com.example.taskapplication.ui.team.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        title = { Text("Create New Team") },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                if (createTeamState is CreateTeamState.Error) {
                    Text(
                        text = createTeamState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedTextField(
                    value = teamName,
                    onValueChange = { teamName = it },
                    label = { Text("Team Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = createTeamState is CreateTeamState.Error
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = teamDescription,
                    onValueChange = { teamDescription = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                if (createTeamState is CreateTeamState.Loading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreateTeam(
                        teamName.trim(),
                        if (teamDescription.isBlank()) null else teamDescription.trim()
                    )
                },
                enabled = teamName.isNotBlank() && createTeamState !is CreateTeamState.Loading
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = createTeamState !is CreateTeamState.Loading
            ) {
                Text("Cancel")
            }
        }
    )
}
