package com.ghostbot.trading.domain.usecase

import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for notification management
 */
class NotificationUseCase @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    
    suspend fun getAllNotifications(): Flow<List<Notification>> {
        return notificationRepository.getAllNotifications()
    }
    
    suspend fun getUnreadNotifications(): Flow<List<Notification>> {
        return notificationRepository.getUnreadNotifications()
    }
    
    suspend fun getNotificationsByType(type: NotificationType): Flow<List<Notification>> {
        return notificationRepository.getNotificationsByType(type)
    }
    
    suspend fun getUnreadCount(): Flow<Int> {
        return notificationRepository.getUnreadCount()
    }
    
    suspend fun markAsRead(notificationId: String): Result<Unit> {
        return notificationRepository.markAsRead(notificationId)
    }
    
    suspend fun markAllAsRead(): Result<Unit> {
        return notificationRepository.markAllAsRead()
    }
    
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return notificationRepository.deleteNotification(notificationId)
    }
    
    suspend fun deleteAllNotifications(): Result<Unit> {
        return notificationRepository.deleteAllNotifications()
    }
    
    suspend fun createSystemNotification(
        title: String,
        message: String,
        priority: NotificationPriority = NotificationPriority.MEDIUM
    ): Result<Unit> {
        val notification = Notification(
            id = System.currentTimeMillis().toString(),
            title = title,
            message = message,
            type = NotificationType.SYSTEM_MESSAGE,
            priority = priority,
            timestamp = System.currentTimeMillis()
        )
        
        return notificationRepository.createNotification(notification)
    }
    
    suspend fun createConnectionStatusNotification(
        isConnected: Boolean
    ): Result<Unit> {
        val message = if (isConnected) {
            "Successfully connected to trading servers"
        } else {
            "Connection to trading servers lost. Attempting to reconnect..."
        }
        
        return notificationRepository.createConnectionNotification(isConnected, message)
    }
    
    suspend fun createMarginCallNotification(
        marginLevel: Float,
        accountBalance: Double
    ): Result<Unit> {
        val notification = Notification(
            id = System.currentTimeMillis().toString(),
            title = "Margin Call Warning",
            message = "Margin level at ${(marginLevel * 100).toInt()}%. Account balance: $$accountBalance",
            type = NotificationType.MARGIN_CALL,
            priority = NotificationPriority.CRITICAL,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "marginLevel" to marginLevel.toString(),
                "balance" to accountBalance.toString()
            )
        )
        
        return notificationRepository.createNotification(notification)
    }
}