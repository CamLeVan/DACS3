# Hướng dẫn sử dụng API Chat Nhóm

## Giới thiệu

Tài liệu này hướng dẫn cách sử dụng API Chat Nhóm trong ứng dụng. API Chat Nhóm cung cấp các chức năng như gửi tin nhắn văn bản, đính kèm tệp, đánh dấu đã đọc, hiển thị trạng thái đang nhập, phản ứng emoji, và đồng bộ hóa tin nhắn khi offline.

## Cấu trúc

Chức năng chat nhóm được triển khai với các thành phần sau:

1. **ChatApiService**: Interface định nghĩa các endpoint API
2. **ChatWebSocketClient**: Client WebSocket để nhận sự kiện thời gian thực
3. **ChatMessageRepositoryImpl**: Triển khai Repository để giao tiếp với API và WebSocket
4. **EnhancedChatViewModel**: ViewModel để quản lý trạng thái và logic của màn hình chat
5. **ChatScreen**: Giao diện người dùng của màn hình chat

## Cách sử dụng

### 1. Khởi tạo các dependency

Các dependency được cung cấp thông qua Dagger Hilt trong `ChatModule`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {
    @Binds
    @Singleton
    abstract fun bindChatMessageRepository(
        chatMessageRepositoryImpl: ChatMessageRepositoryImpl
    ): MessageRepository

    companion object {
        @Provides
        @Singleton
        fun provideChatApiService(retrofit: Retrofit): ChatApiService {
            return retrofit.create(ChatApiService::class.java)
        }

        @Provides
        @Singleton
        fun provideChatWebSocketClient(okHttpClient: OkHttpClient): ChatWebSocketClient {
            return ChatWebSocketClient(okHttpClient)
        }
    }
}
```

### 2. Sử dụng EnhancedChatViewModel

```kotlin
@Composable
fun ChatScreen(
    viewModel: EnhancedChatViewModel = hiltViewModel(),
    teamId: String,
    onBackClick: () -> Unit
) {
    // ...
}
```

### 3. Gửi tin nhắn

```kotlin
// Trong ViewModel
fun sendMessage() {
    val text = _messageText.value.trim()
    if (text.isEmpty() && _attachments.value.isEmpty()) return

    viewModelScope.launch {
        _sendMessageState.value = SendMessageState.Sending

        val currentUserId = _currentUserId.value ?: return@launch
        val clientTempId = UUID.randomUUID().toString()
        val currentAttachments = _attachments.value

        // Gửi tin nhắn mới
        messageRepository.sendTeamMessage(teamId, text, clientTempId, currentAttachments)
            .onSuccess {
                _sendMessageState.value = SendMessageState.Success
                _messageText.value = ""
                _attachments.value = emptyList()

                // Gửi trạng thái không còn đang nhập
                sendTypingStatus(false)
            }
            .onFailure { e ->
                _sendMessageState.value = SendMessageState.Error(e.message ?: "Không thể gửi tin nhắn")
            }
    }
}
```

### 4. Nhận tin nhắn thời gian thực

WebSocket sẽ tự động nhận tin nhắn mới và cập nhật vào cơ sở dữ liệu cục bộ. ViewModel sẽ quan sát các thay đổi và cập nhật UI.

### 5. Chỉnh sửa tin nhắn

```kotlin
// Trong ViewModel
fun editMessage(messageId: String, newContent: String) {
    viewModelScope.launch {
        messageRepository.editMessage(messageId, newContent)
            .onSuccess {
                // Xử lý thành công
            }
            .onFailure { e ->
                // Xử lý lỗi
            }
    }
}
```

### 6. Xóa tin nhắn

```kotlin
// Trong ViewModel
fun deleteMessage(messageId: String) {
    viewModelScope.launch {
        messageRepository.deleteMessage(messageId)
            .onFailure { e ->
                // Xử lý lỗi
            }
    }
}
```

### 7. Gửi trạng thái đang nhập

```kotlin
// Trong ViewModel
fun sendTypingStatus(isTyping: Boolean) {
    viewModelScope.launch {
        messageRepository.sendTypingStatus(teamId, isTyping)
    }
}
```

### 8. Thêm phản ứng emoji

```kotlin
// Trong ViewModel
fun addReaction(messageId: String, reaction: String) {
    viewModelScope.launch {
        messageRepository.addReaction(messageId, reaction)
            .onFailure { e ->
                // Xử lý lỗi
            }
    }
}
```

## Xử lý offline

Khi không có kết nối mạng, ứng dụng sẽ:

1. Lưu tin nhắn vào cơ sở dữ liệu cục bộ với trạng thái `pending_create`
2. Hiển thị tin nhắn trong UI với trạng thái đang chờ
3. Khi có kết nối mạng, tự động đồng bộ tin nhắn lên server
4. Cập nhật trạng thái tin nhắn thành `synced` sau khi đồng bộ thành công

## Lưu ý

1. Đảm bảo đã đăng nhập và có token xác thực trước khi sử dụng API
2. Xử lý các trường hợp lỗi khi gọi API
3. Đảm bảo ngắt kết nối WebSocket khi không cần thiết để tiết kiệm tài nguyên

## Tài liệu tham khảo

- [Tài liệu API Chat Nhóm](link_to_api_documentation)
- [Retrofit Documentation](https://square.github.io/retrofit/)
- [OkHttp WebSocket](https://square.github.io/okhttp/4.x/okhttp/okhttp3/-web-socket/)
