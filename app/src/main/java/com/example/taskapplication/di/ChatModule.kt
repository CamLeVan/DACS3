package com.example.taskapplication.di

import com.example.taskapplication.data.api.ChatApiService
import com.example.taskapplication.data.repository.ChatMessageRepositoryImpl
import com.example.taskapplication.data.websocket.ChatWebSocketClient
import com.example.taskapplication.domain.repository.MessageRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Module cung cấp các dependency liên quan đến chat
 */
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
