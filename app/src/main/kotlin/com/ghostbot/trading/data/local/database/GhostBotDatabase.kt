package com.ghostbot.trading.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.ghostbot.trading.data.local.converters.TypeConverters as GhostBotTypeConverters
import com.ghostbot.trading.data.local.dao.*
import com.ghostbot.trading.data.local.entity.*

@Database(
    entities = [
        TradeEntity::class,
        NotificationEntity::class,
        AISignalEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(GhostBotTypeConverters::class)
abstract class GhostBotDatabase : RoomDatabase() {
    
    abstract fun tradeDao(): TradeDao
    abstract fun notificationDao(): NotificationDao
    abstract fun aiSignalDao(): AISignalDao
    
    companion object {
        private const val DATABASE_NAME = "ghost_bot_database"
        
        @Volatile
        private var INSTANCE: GhostBotDatabase? = null
        
        fun getInstance(context: Context): GhostBotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GhostBotDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}