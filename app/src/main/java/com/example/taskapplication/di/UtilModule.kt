package com.example.taskapplication.di

import android.content.Context
import com.example.taskapplication.data.util.ConnectionChecker
import com.example.taskapplication.data.util.DataStoreManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UtilModule {

    @Provides
    @Singleton
    fun provideDataStoreManager(
        @ApplicationContext context: Context
    ): DataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideConnectionChecker(
        @ApplicationContext context: Context
    ): ConnectionChecker {
        return ConnectionChecker(context)
    }
} 