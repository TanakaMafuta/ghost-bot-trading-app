package com.ghostbot.trading.domain.repository

import com.ghostbot.trading.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for notification management
 */
interface NotificationRepository {
    
    // Basic Operations
    suspend fun getAllNotifications(): Flow<List<Notification>>
    suspend fun getUnreadNotifications(): Flow<List<Notification>>
    suspend fun getNotificationsByType(type: NotificationType): Flow<List<Notification>>
    suspend fun getUnreadCount(): Flow<Int>
    
    // Notification Management
    suspend fun createNotification(notification: Notification): Result<Unit>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    suspend fun deleteAllNotifications(): Result<Unit>
    
    // Specialized Notifications
    suspend fun createTradeNotification(
        message: String,
        tradeId: String,
        profit: Double
    ): Result<Unit>
    
    suspend fun createAISignalNotification(
        symbol: String,
        signal: String,
        confidence: Float
    ): Result<Unit>
    
    suspend fun createConnectionNotification(
        isConnected: Boolean,
        message: String
    ): Result<Unit>
}