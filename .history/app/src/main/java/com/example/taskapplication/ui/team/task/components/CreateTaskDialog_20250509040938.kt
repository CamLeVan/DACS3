package com.example.taskapplication.ui.team.task.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.taskapplication.domain.model.TeamMember
import com.example.taskapplication.ui.components.DatePickerDialog
import com.example.taskapplication.ui.team.task.CreateTaskState
import com.example.taskapplication.ui.team.task.TeamMembersState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog for creating a new team task
 */
@Composable
fun CreateTaskDialog(
    createTaskState: CreateTaskState,
    teamMembersState: TeamMembersState,
    onDismiss: () -> Unit,
    onCreateTask: (title: String, description: String?, dueDate: Long?, priority: Int, assignedUserId: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableLongStateOf(0L) }
    var priority by remember { mutableStateOf(1) } // Default priority: Medium
    var assignedUserId by remember { mutableStateOf<String?>(null) }
    var assigneeName by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showAssigneeDropdown by remember { mutableStateOf(false) }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Task") },
        text = {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (createTaskState is CreateTaskState.Error) {
                    Text(
                        text = createTaskState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = createTaskState is CreateTaskState.Error
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Due date field
                OutlinedTextField(
                    value = if (dueDate > 0) dateFormatter.format(Date(dueDate)) else "",
                    onValueChange = { },
                    label = { Text("Due Date (Optional)") },
                    readOnly = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable { showDatePicker = true }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Priority field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Priority:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPriorityDropdown = true }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = when (priority) {
                                    0 -> "Low"
                                    1 -> "Medium"
                                    2 -> "High"
                                    else -> "Medium"
                                }
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showPriorityDropdown,
                            onDismissRequest = { showPriorityDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Low") },
                                onClick = {
                                    priority = 0
                                    showPriorityDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Medium") },
                                onClick = {
                                    priority = 1
                                    showPriorityDropdown = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("High") },
                                onClick = {
                                    priority = 2
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Assignee field
                if (teamMembersState is TeamMembersState.Success) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Assign to:",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAssigneeDropdown = true }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = assigneeName.ifEmpty { "Unassigned" }
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = showAssigneeDropdown,
                                onDismissRequest = { showAssigneeDropdown = false }
                            ) {
                                // Unassigned option
                                DropdownMenuItem(
                                    text = { Text("Unassigned") },
                                    onClick = {
                                        assignedUserId = null
                                        assigneeName = ""
                                        showAssigneeDropdown = false
                                    }
                                )

                                // Team members
                                teamMembersState.members.forEach { member ->
                                    DropdownMenuItem(
                                        text = { Text(member.userId) },
                                        onClick = {
                                            assignedUserId = member.userId
                                            assigneeName = member.userId
                                            showAssigneeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (createTaskState is CreateTaskState.Loading) {
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
                    onCreateTask(
                        title.trim(),
                        if (description.isBlank()) null else description.trim(),
                        if (dueDate > 0) dueDate else null,
                        priority,
                        assignedUserId
                    )
                },
                enabled = title.isNotBlank() && createTaskState !is CreateTaskState.Loading
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = createTaskState !is CreateTaskState.Loading
            ) {
                Text("Cancel")
            }
        }
    )

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDateSelected = {
                dueDate = it
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
