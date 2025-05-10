package com.example.taskapplication.ui.team.kanban.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.taskapplication.domain.model.TeamMember
import com.example.taskapplication.ui.team.kanban.TaskFilter

/**
 * Dialog for filtering tasks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFilterDialog(
    onDismiss: () -> Unit,
    onApplyFilter: (assignedUserId: String?, priority: String?, isCompleted: Boolean?) -> Unit,
    teamMembers: List<TeamMember>,
    currentFilter: TaskFilter
) {
    var assignedUserId by remember { mutableStateOf(currentFilter.assignedUserId) }
    var priority by remember { mutableStateOf(currentFilter.priority) }
    var isCompleted by remember { mutableStateOf(currentFilter.isCompleted) }
    
    var expandedAssignee by remember { mutableStateOf(false) }
    var expandedPriority by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Filter Tasks",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Assignee filter
                ExposedDropdownMenuBox(
                    expanded = expandedAssignee,
                    onExpandedChange = { expandedAssignee = it }
                ) {
                    OutlinedTextField(
                        value = teamMembers.find { it.userId == assignedUserId }?.name ?: "All Members",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Assigned To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAssignee) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedAssignee,
                        onDismissRequest = { expandedAssignee = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Members") },
                            onClick = {
                                assignedUserId = null
                                expandedAssignee = false
                            }
                        )
                        
                        teamMembers.forEach { member ->
                            DropdownMenuItem(
                                text = { Text(member.name) },
                                onClick = {
                                    assignedUserId = member.userId
                                    expandedAssignee = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Priority filter
                ExposedDropdownMenuBox(
                    expanded = expandedPriority,
                    onExpandedChange = { expandedPriority = it }
                ) {
                    OutlinedTextField(
                        value = priority ?: "All Priorities",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Priority") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPriority) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedPriority,
                        onDismissRequest = { expandedPriority = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Priorities") },
                            onClick = {
                                priority = null
                                expandedPriority = false
                            }
                        )
                        
                        listOf("HIGH", "MEDIUM", "LOW").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    priority = option
                                    expandedPriority = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Completion status filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Completion Status")
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted == true,
                            onCheckedChange = {
                                isCompleted = if (it) true else null
                            }
                        )
                        
                        Text("Completed")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isCompleted == false,
                            onCheckedChange = {
                                isCompleted = if (it) false else null
                            }
                        )
                        
                        Text("Incomplete")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            onApplyFilter(null, null, null)
                            onDismiss()
                        }
                    ) {
                        Text("Clear Filters")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            onApplyFilter(assignedUserId, priority, isCompleted)
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
