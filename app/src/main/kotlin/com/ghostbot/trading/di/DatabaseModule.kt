package com.ghostbot.trading.di

import android.content.Context
import androidx.room.Room
import com.ghostbot.trading.data.local.database.GhostBotDatabase
import com.ghostbot.trading.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideGhostBotDatabase(
        @ApplicationContext context: Context
    ): GhostBotDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            GhostBotDatabase::class.java,
            "ghost_bot_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideTradeDao(database: GhostBotDatabase): TradeDao {
        return database.tradeDao()
    }
    
    @Provides
    fun provideNotificationDao(database: GhostBotDatabase): NotificationDao {
        return database.notificationDao()
    }
    
    @Provides
    fun provideAISignalDao(database: GhostBotDatabase): AISignalDao {
        return database.aiSignalDao()
    }
}