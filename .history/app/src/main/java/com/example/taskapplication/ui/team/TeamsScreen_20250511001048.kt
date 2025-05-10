package com.example.taskapplication.ui.team

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.taskapplication.ui.animation.AnimationUtils
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
                title = { Text("Nhóm") }
            )
        },
        floatingActionButton = {
            // Floating action button with animation
            val fabScale = remember { Animatable(1f) }

            LaunchedEffect(Unit) {
                // Pulsating animation for FAB
                while (true) {
                    fabScale.animateTo(
                        targetValue = 1.1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        )
                    )
                    fabScale.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = 1000,
                            easing = FastOutSlowInEasing
                        )
                    )
                    delay(1000) // Pause between pulses
                }
            }

            FloatingActionButton(
                onClick = { viewModel.showCreateTeamDialog() },
                modifier = Modifier.graphicsLayer {
                    scaleX = fabScale.value
                    scaleY = fabScale.value
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo nhóm mới")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Loading state with animation
            AnimatedVisibility(
                visible = teamsState is TeamsState.Loading,
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Rotating animation for loading indicator
                        val rotation = rememberInfiniteTransition().animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "Loading Rotation"
                        )

                        CircularProgressIndicator(
                            modifier = Modifier.graphicsLayer { rotationZ = rotation.value }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pulsating text animation
                        val scale = rememberInfiniteTransition().animateFloat(
                            initialValue = 0.95f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Text Pulse"
                        )

                        Text(
                            text = "Đang tải danh sách nhóm...",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                            }
                        )
                    }
                }
            }

            // Empty state with animation
            AnimatedVisibility(
                visible = teamsState is TeamsState.Empty,
                enter = fadeIn(tween(500)) + expandIn(tween(500), expandFrom = Alignment.Center),
                exit = fadeOut(tween(300)) + shrinkOut(tween(300), shrinkTowards = Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Bouncing animation for empty state
                    val bounce = rememberInfiniteTransition().animateFloat(
                        initialValue = 0f,
                        targetValue = 10f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Empty State Bounce"
                    )

                    Text(
                        text = "Bạn chưa có nhóm nào",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.graphicsLayer {
                            translationY = bounce.value
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.showCreateTeamDialog() },
                        modifier = Modifier.graphicsLayer {
                            scaleX = 1f + (bounce.value / 100f)
                            scaleY = 1f + (bounce.value / 100f)
                        }
                    ) {
                        Text("Tạo nhóm mới")
                    }
                }
            }

            // Success state with team list
            AnimatedVisibility(
                visible = teamsState is TeamsState.Success,
                enter = fadeIn(tween(500)),
                exit = fadeOut(tween(300))
            ) {
                if (teamsState is TeamsState.Success) {
                    val teams = teamsState.teams
                    LazyColumn {
                        itemsIndexed(teams) { index, team ->
                            AnimatedVisibility(
                                visible = true,
                                enter = AnimationUtils.listItemEnterAnimation(index),
                                exit = AnimationUtils.listItemExitAnimation
                            ) {
                                TeamItem(
                                    team = team,
                                    onClick = { onTeamClick(team.id) }
                                )
                            }
                        }
                    }
                }
            }

            // Error state with animation
            AnimatedVisibility(
                visible = teamsState is TeamsState.Error,
                enter = fadeIn(tween(500)) + expandIn(tween(500), expandFrom = Alignment.Center),
                exit = fadeOut(tween(300)) + shrinkOut(tween(300), shrinkTowards = Alignment.Center)
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Shaking animation for error state
                    val shake = rememberInfiniteTransition().animateFloat(
                        initialValue = -5f,
                        targetValue = 5f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Error Shake"
                    )

                    Text(
                        text = "Không thể tải danh sách nhóm",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.graphicsLayer {
                            translationX = shake.value
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.loadTeams() },
                        modifier = Modifier.graphicsLayer {
                            // Subtle pulse for retry button
                            val pulse = rememberInfiniteTransition().animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Retry Button Pulse"
                            ).value
                            scaleX = pulse
                            scaleY = pulse
                        }
                    ) {
                        Text("Thử lại")
                    }
                }
            }
        }
    }

    // Show create team dialog with animation
    AnimatedVisibility(
        visible = showCreateTeamDialog,
        enter = AnimationUtils.dialogEnterAnimation,
        exit = AnimationUtils.dialogExitAnimation
    ) {
        CreateTeamDialog(
            createTeamState = createTeamState,
            onDismiss = { viewModel.hideCreateTeamDialog() },
            onCreateTeam = { name, description ->
                viewModel.createTeam(name, description)
            }
        )
    }
}
