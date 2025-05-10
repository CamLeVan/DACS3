package com.example.taskapplication.ui.team

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.ui.team.components.CreateTeamDialog
import com.example.taskapplication.ui.team.components.TeamItem

/**
 * Screen that displays a list of teams the user belongs to
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamsScreen(
    viewModel: TeamViewModel = hiltViewModel(),
    onTeamClick: (String) -> Unit
) {
    val teamsState by viewModel.teamsState.collectAsState()
    val showCreateTeamDialog by viewModel.showCreateTeamDialog.collectAsState()
    val createTeamState by viewModel.createTeamState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Show error message in snackbar
    LaunchedEffect(teamsState) {
        if (teamsState is TeamsState.Error) {
            snackbarHostState.showSnackbar(
                message = (teamsState as TeamsState.Error).message
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teams") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateTeamDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Create Team")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (teamsState) {
                is TeamsState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is TeamsState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You don't have any teams yet",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.showCreateTeamDialog() }) {
                            Text("Create a team")
                        }
                    }
                }

                is TeamsState.Success -> {
                    val teams = (teamsState as TeamsState.Success).teams
                    LazyColumn {
                        items(teams) { team ->
                            TeamItem(
                                team = team,
                                onClick = { onTeamClick(team.id) }
                            )
                        }
                    }
                }

                is TeamsState.Error -> {
                    // Error is shown in snackbar, but we still need to show content
                    // We can either show empty state or retry button
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Failed to load teams",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { viewModel.loadTeams() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }

    // Show create team dialog
    if (showCreateTeamDialog) {
        CreateTeamDialog(
            createTeamState = createTeamState,
            onDismiss = { viewModel.hideCreateTeamDialog() },
            onCreateTeam = { name, description ->
                viewModel.createTeam(name, description)
            }
        )
    }
}
