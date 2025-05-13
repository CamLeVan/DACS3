package com.example.taskapplication.ui.team.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.taskapplication.domain.model.Message
import com.example.taskapplication.ui.components.ConfirmationDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    teamId: String,
    onBackClick: () -> Unit
) {
    val messagesState by viewModel.messagesState.collectAsState()
    val sendMessageState by viewModel.sendMessageState.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val teamName by viewModel.teamName.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val typingUsers by viewModel.typingUsers.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val editingMessage by viewModel.editingMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Scroll to bottom when new messages arrive
    val messages = if (messagesState is MessagesState.Success) {
        (messagesState as MessagesState.Success).messages
    } else {
        emptyList()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    // Show error message in snackbar
    LaunchedEffect(sendMessageState) {
        if (sendMessageState is SendMessageState.Error) {
            snackbarHostState.showSnackbar(
                message = (sendMessageState as SendMessageState.Error).message
            )
        }
    }

    var messageToDelete by remember { mutableStateOf<Message?>(null) }

    // Background gradient for chat
    val chatBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = teamName ?: "Team Chat",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "${messages.size} messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Show menu */ }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                // Show typing indicator
                if (typingUsers.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        val typingNames = typingUsers.keys.take(2).joinToString(", ")
                        val additionalCount = (typingUsers.size - 2).coerceAtLeast(0)
                        val typingText = when {
                            additionalCount > 0 -> "$typingNames and $additionalCount more are typing..."
                            typingUsers.size > 1 -> "$typingNames are typing..."
                            else -> "$typingNames is typing..."
                        }

                        Text(
                            text = typingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontStyle = FontStyle.Italic
                        )
                    }
                }

                // Clean up expired typing statuses every 5 seconds
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(5000)
                        viewModel.cleanupTypingStatuses()
                    }
                }

                MessageInput(
                    value = messageText,
                    onValueChange = { viewModel.updateMessageText(it) },
                    onSendClick = { viewModel.sendMessage() },
                    isLoading = sendMessageState is SendMessageState.Sending,
                    attachments = attachments,
                    onAddAttachment = { /* TODO: Implement attachment picker */ },
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    isEditing = editingMessage != null,
                    onCancelEdit = { viewModel.cancelEditingMessage() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(chatBackground)
                .padding(paddingValues)
        ) {
            when (val state = messagesState) {
                is MessagesState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Đang tải tin nhắn...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                is MessagesState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Empty state illustration
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "No messages yet",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Start the conversation by sending a message to your team",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                is MessagesState.Success -> {
                    LazyColumn(
                        state = listState,
                        reverseLayout = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        itemsIndexed(state.messages) { index, message ->
                            val isCurrentUser = message.senderId == currentUserId

                            // Create a transition state for animation
                            val visibleState = remember(message.id) {
                                MutableTransitionState(false).apply {
                                    // Start the animation immediately
                                    targetState = true
                                }
                            }

                            // Animate message appearance
                            AnimatedVisibility(
                                visibleState = visibleState,
                                enter = if (isCurrentUser) {
                                    slideInHorizontally(
                                        initialOffsetX = { it / 3 },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) + fadeIn(animationSpec = tween(200))
                                } else {
                                    slideInHorizontally(
                                        initialOffsetX = { -it / 3 },
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) + fadeIn(animationSpec = tween(200))
                                },
                                exit = fadeOut()
                            ) {
                                MessageItem(
                                    message = message,
                                    isCurrentUser = isCurrentUser,
                                    onDeleteClick = { messageToDelete = message },
                                    onEditClick = { viewModel.startEditingMessage(message) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                )
                            }

                            // Add date separator if needed
                            if (index < state.messages.size - 1) {
                                val currentDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .format(Date(message.timestamp))
                                val nextDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                    .format(Date(state.messages[index + 1].timestamp))

                                if (currentDate != nextDate) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = currentDate,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                is MessagesState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete, // Sử dụng biểu tượng lỗi phù hợp hơn
                                contentDescription = "Lỗi",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Đã xảy ra lỗi!",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            TextButton(
                                onClick = { viewModel.loadMessages() },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Thử lại")
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for deleting message
    messageToDelete?.let { message ->
        ConfirmationDialog(
            title = "Xóa tin nhắn",
            message = "Bạn có chắc chắn muốn xóa tin nhắn này? Hành động này không thể hoàn tác.",
            confirmButtonText = "Xóa",
            onConfirm = {
                viewModel.deleteMessage(message.id)
                messageToDelete = null
            },
            onDismiss = {
                messageToDelete = null
            }
        )
    }
}

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = dateFormatter.format(Date(message.timestamp))

    // Different colors for current user vs others
    val bubbleColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (isCurrentUser) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // Different bubble shapes for current user vs others
    val bubbleShape = if (isCurrentUser) {
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    } else {
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
    }

    // Scale animation for hover effect
    var isHovered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isHovered) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "Message Scale Animation"
    )

    // Show options menu
    var showOptions by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isCurrentUser) {
            // Avatar placeholder (only for other users)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (message.senderName?.take(1) ?: message.senderId.take(1)).uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (!isCurrentUser) {
                // Sender name (only for other users)
                Text(
                    text = message.senderName ?: message.senderId,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(2.dp))
            }

            // Message content with time
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCurrentUser) {
                    // Time for current user (left side)
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 4.dp, bottom = 4.dp)
                    )
                }

                // Message bubble
                Card(
                    shape = bubbleShape,
                    colors = CardDefaults.cardColors(containerColor = bubbleColor),
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .shadow(
                            elevation = 1.dp,
                            shape = bubbleShape,
                            clip = true
                        )
                        .clickable { isHovered = !isHovered }
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        if (message.isDeleted) {
                            Text(
                                text = "Tin nhắn này đã bị xóa",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.7f),
                                fontStyle = FontStyle.Italic
                            )
                        } else {
                            // Message content
                            Text(
                                text = message.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor
                            )

                            // Attachments
                            if (message.attachments.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Column {
                                    message.attachments.forEach { attachment ->
                                        AttachmentItem(
                                            attachment = attachment,
                                            textColor = textColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }

                            // Message actions for current user
                            if (isCurrentUser && isHovered && !message.isDeleted) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    IconButton(
                                        onClick = onEditClick,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit message",
                                            tint = textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    IconButton(
                                        onClick = onDeleteClick,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete message",
                                            tint = textColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Edited indicator
                            if (message.lastModified > message.createdAt + 60000) { // 1 minute difference
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "(đã chỉnh sửa)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontStyle = FontStyle.Italic
                                    ),
                                    color = textColor.copy(alpha = 0.5f)
                                )
                            }

                            // Pending status
                            if (message.syncStatus == "pending_create" || message.syncStatus == "pending_update") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Pending",
                                        tint = textColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Đang gửi...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textColor.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (!isCurrentUser) {
                    // Time for other users (right side)
                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }
            }

            // Delete option (only for current user)
            if (isCurrentUser && !message.isDeleted) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Xóa",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onDeleteClick() }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun MessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    attachments: List<com.example.taskapplication.domain.model.Attachment> = emptyList(),
    onAddAttachment: () -> Unit = {},
    onRemoveAttachment: (String) -> Unit = {},
    isEditing: Boolean = false,
    onCancelEdit: () -> Unit = {}
) {
    var showEmojiPicker by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Emoji picker (simplified version)
        AnimatedVisibility(
            visible = showEmojiPicker,
            enter = expandVertically() + fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Chọn biểu tượng cảm xúc",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(8.dp)
                    )

                    Divider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Simple emoji grid (would be replaced with a proper emoji picker in a real app)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val emojis = listOf("😊", "👍", "❤️", "😂", "🎉", "👏", "🔥", "✅")
                        emojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onValueChange(value + emoji)
                                        showEmojiPicker = false
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Attachment button
            IconButton(
                onClick = onAddAttachment,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Add attachment",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            // Show attachments if any
            if (attachments.isNotEmpty()) {
                Text(
                    text = "${attachments.size} files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Show editing indicator
            if (isEditing) {
                Text(
                    text = "Editing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable(onClick = onCancelEdit)
                )
            }

            // Emoji button
            IconButton(
                onClick = { showEmojiPicker = !showEmojiPicker },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "Add emoji",
                    tint = if (showEmojiPicker)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                )
            }

            // Text field
            TextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = {
                    Text(
                        "Nhập tin nhắn",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send button with animation
            val sendButtonColor = if (value.isBlank() && attachments.isEmpty()) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.primary
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(sendButtonColor)
                    .clickable(enabled = (value.isNotBlank() || attachments.isNotEmpty()) && !isLoading) {
                        onSendClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Message",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AttachmentItem(
    attachment: com.example.taskapplication.domain.model.Attachment,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.05f))
            .padding(8.dp)
    ) {
        // File icon
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = null,
            tint = textColor.copy(alpha = 0.7f),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // File info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = attachment.fileName,
                style = MaterialTheme.typography.bodySmall,
                color = textColor,
                maxLines = 1
            )

            Text(
                text = formatFileSize(attachment.fileSize),
                style = MaterialTheme.typography.bodySmall,
                color = textColor.copy(alpha = 0.7f)
            )
        }
    }
}

// Helper function to format file size
private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> "${size / (1024 * 1024)} MB"
    }
}