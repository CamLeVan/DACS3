package com.example.taskapplication.ui.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.taskapplication.ui.animation.AnimationUtils
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutClick: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Title with animation
                    val titleScale = remember { Animatable(0.8f) }

                    LaunchedEffect(Unit) {
                        titleScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }

                    Text(
                        "Hồ sơ",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.graphicsLayer {
                            scaleX = titleScale.value
                            scaleY = titleScale.value
                        }
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    // Logout icon with animation
                    var isHovered by remember { mutableStateOf(false) }
                    val rotation by animateFloatAsState(
                        targetValue = if (isHovered) 20f else 0f,
                        animationSpec = tween(200),
                        label = "Logout Icon Rotation"
                    )

                    IconButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Đăng xuất",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.graphicsLayer {
                                rotationZ = rotation
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            // Loading state with animation
            AnimatedVisibility(
                visible = uiState is ProfileUiState.Loading,
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
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
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
                            text = "Đang tải thông tin...",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.graphicsLayer {
                                scaleX = scale.value
                                scaleY = scale.value
                            }
                        )
                    }
                }
            }

            // Success state with profile content
            AnimatedVisibility(
                visible = uiState is ProfileUiState.Success,
                enter = fadeIn(tween(500)) + expandIn(tween(500), expandFrom = Alignment.Center),
                exit = fadeOut(tween(300)) + shrinkOut(tween(300), shrinkTowards = Alignment.Center)
            ) {
                if (uiState is ProfileUiState.Success) {
                    val user = uiState.user
                    ProfileContent(user = user, onLogoutClick = onLogoutClick)
                }
            }

            // Error state with animation
            AnimatedVisibility(
                visible = uiState is ProfileUiState.Error,
                enter = fadeIn(tween(500)) + expandIn(tween(500), expandFrom = Alignment.Center),
                exit = fadeOut(tween(300)) + shrinkOut(tween(300), shrinkTowards = Alignment.Center)
            ) {
                if (uiState is ProfileUiState.Error) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Shaking animation for error card
                        val shake = rememberInfiniteTransition().animateFloat(
                            initialValue = -5f,
                            targetValue = 5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "Error Shake"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .graphicsLayer { translationX = shake.value },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = uiState.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: com.example.taskapplication.domain.model.User,
    onLogoutClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header with gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Image with animation
                var isAvatarHovered by remember { mutableStateOf(false) }
                val avatarScale by animateFloatAsState(
                    targetValue = if (isAvatarHovered) 1.05f else 1f,
                    animationSpec = tween(200),
                    label = "Avatar Scale Animation"
                )
                val avatarElevation by animateDpAsState(
                    targetValue = if (isAvatarHovered) 8.dp else 4.dp,
                    animationSpec = tween(200),
                    label = "Avatar Elevation Animation"
                )

                // Initial appear animation
                val initialScale = remember { Animatable(0.5f) }
                LaunchedEffect(Unit) {
                    initialScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .graphicsLayer {
                            scaleX = initialScale.value * avatarScale
                            scaleY = initialScale.value * avatarScale
                        }
                        .shadow(
                            elevation = avatarElevation,
                            shape = CircleShape
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                        .padding(4.dp)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    isAvatarHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (user.avatar.isNullOrEmpty()) {
                        // Rotating animation for default avatar
                        val rotation = if (isAvatarHovered) {
                            rememberInfiniteTransition().animateFloat(
                                initialValue = -5f,
                                targetValue = 5f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "Avatar Rotation"
                            )
                        } else {
                            remember { Animatable(0f) }
                        }

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Ảnh đại diện",
                            modifier = Modifier
                                .size(100.dp)
                                .padding(8.dp)
                                .graphicsLayer {
                                    rotationZ = if (isAvatarHovered) rotation.value else 0f
                                },
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.avatar)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Ảnh đại diện",
                            modifier = Modifier
                                .size(132.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Name with animation
                val nameScale = remember { Animatable(0.8f) }
                LaunchedEffect(Unit) {
                    delay(200) // Delay to create a staggered effect
                    nameScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        scaleX = nameScale.value
                        scaleY = nameScale.value
                        alpha = nameScale.value
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // User Email with icon and animation
                val emailScale = remember { Animatable(0.8f) }
                LaunchedEffect(Unit) {
                    delay(400) // Delay to create a staggered effect
                    emailScale.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        scaleX = emailScale.value
                        scaleY = emailScale.value
                        alpha = emailScale.value
                    }
                ) {
                    // Pulsating email icon
                    val iconPulse = rememberInfiniteTransition().animateFloat(
                        initialValue = 0.9f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "Email Icon Pulse"
                    )

                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(16.dp)
                            .graphicsLayer {
                                scaleX = iconPulse.value
                                scaleY = iconPulse.value
                            }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Profile Info Cards with animation
        val cardScale = remember { Animatable(0.9f) }
        LaunchedEffect(Unit) {
            delay(600) // Delay to create a staggered effect
            cardScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        var isCardHovered by remember { mutableStateOf(false) }
        val hoverScale by animateFloatAsState(
            targetValue = if (isCardHovered) 1.02f else 1f,
            animationSpec = tween(200),
            label = "Card Hover Scale"
        )
        val hoverElevation by animateDpAsState(
            targetValue = if (isCardHovered) 8.dp else 4.dp,
            animationSpec = tween(200),
            label = "Card Hover Elevation"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .graphicsLayer {
                    scaleX = cardScale.value * hoverScale
                    scaleY = cardScale.value * hoverScale
                    alpha = cardScale.value
                }
                .shadow(
                    elevation = hoverElevation,
                    shape = RoundedCornerShape(16.dp)
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            isCardHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Thông tin tài khoản",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Staggered animation for info items
                ProfileInfoItem(title = "ID người dùng", value = user.id, index = 0)
                ProfileInfoItem(title = "Loại tài khoản", value = "Tiêu chuẩn", index = 1)
                ProfileInfoItem(title = "Ngày tham gia", value = "Tháng 1, 2023", index = 2)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Logout Button with animation
        val logoutButtonScale = remember { Animatable(0.9f) }
        LaunchedEffect(Unit) {
            delay(800) // Delay to create a staggered effect
            logoutButtonScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        var isButtonHovered by remember { mutableStateOf(false) }
        val buttonHoverScale by animateFloatAsState(
            targetValue = if (isButtonHovered) 1.05f else 1f,
            animationSpec = tween(200),
            label = "Button Hover Scale"
        )

        // Pulsating effect for logout button
        val buttonPulse = rememberInfiniteTransition().animateFloat(
            initialValue = 0.98f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "Button Pulse"
        )

        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .graphicsLayer {
                    scaleX = logoutButtonScale.value * buttonHoverScale * buttonPulse.value
                    scaleY = logoutButtonScale.value * buttonHoverScale * buttonPulse.value
                    alpha = logoutButtonScale.value
                }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            isButtonHovered = event.type == PointerEventType.Enter || event.type == PointerEventType.Move
                        }
                    }
                },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            // Rotating icon animation
            val iconRotation by animateFloatAsState(
                targetValue = if (isButtonHovered) 180f else 0f,
                animationSpec = tween(300),
                label = "Icon Rotation"
            )

            Icon(
                imageVector = Icons.Default.ExitToApp,
                contentDescription = "Đăng xuất",
                modifier = Modifier.graphicsLayer {
                    rotationZ = iconRotation
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Đăng xuất",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ProfileInfoItem(title: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Divider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp
        )
    }
}
