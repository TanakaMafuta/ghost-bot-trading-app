package com.ghostbot.trading.data.repository

import com.ghostbot.trading.data.local.dao.NotificationDao
import com.ghostbot.trading.data.local.entity.NotificationEntity
import com.ghostbot.trading.domain.model.Notification
import com.ghostbot.trading.domain.model.NotificationPriority
import com.ghostbot.trading.domain.model.NotificationType
import com.ghostbot.trading.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val json: Json
) : NotificationRepository {
    
    override suspend fun getAllNotifications(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications().map { entities ->
            entities.map { entity -> entity.toDomainModel() }
        }
    }
    
    override suspend fun getUnreadNotifications(): Flow<List<Notification>> {
        return notificationDao.getUnreadNotifications().map { entities ->
            entities.map { entity -> entity.toDomainModel() }
        }
    }
    
    override suspend fun getNotificationsByType(type: NotificationType): Flow<List<Notification>> {
        return notificationDao.getNotificationsByType(type).map { entities ->
            entities.map { entity -> entity.toDomainModel() }
        }
    }
    
    override suspend fun getUnreadCount(): Flow<Int> {
        return notificationDao.getUnreadCount()
    }
    
    override suspend fun createNotification(notification: Notification): Result<Unit> {
        return try {
            val entity = NotificationEntity(
                id = notification.id,
                title = notification.title,
                message = notification.message,
                type = notification.type,
                priority = notification.priority,
                timestamp = notification.timestamp,
                isRead = notification.isRead,
                actionUrl = notification.actionUrl,
                metadata = json.encodeToString(notification.metadata)
            )
            
            notificationDao.insertNotification(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create notification")
            Result.failure(e)
        }
    }
    
    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationDao.markAsRead(notificationId)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark notification as read")
            Result.failure(e)
        }
    }
    
    override suspend fun markAllAsRead(): Result<Unit> {
        return try {
            notificationDao.markAllAsRead()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to mark all notifications as read")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            val entity = NotificationEntity(
                id = notificationId,
                title = "",
                message = "",
                type = NotificationType.SYSTEM_MESSAGE,
                priority = NotificationPriority.LOW,
                timestamp = 0
            )
            notificationDao.deleteNotification(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete notification")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAllNotifications(): Result<Unit> {
        return try {
            notificationDao.deleteAllNotifications()
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete all notifications")
            Result.failure(e)
        }
    }
    
    override suspend fun createTradeNotification(
        message: String,
        tradeId: String,
        profit: Double
    ): Result<Unit> {
        val notification = Notification(
            id = System.currentTimeMillis().toString(),
            title = "Trade Executed",
            message = message,
            type = NotificationType.TRADE_EXECUTED,
            priority = if (profit >= 0) NotificationPriority.MEDIUM else NotificationPriority.HIGH,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "tradeId" to tradeId,
                "profit" to profit.toString()
            )
        )
        
        return createNotification(notification)
    }
    
    override suspend fun createAISignalNotification(
        symbol: String,
        signal: String,
        confidence: Float
    ): Result<Unit> {
        val notification = Notification(
            id = System.currentTimeMillis().toString(),
            title = "AI Signal Alert",
            message = "$signal signal for $symbol with ${(confidence * 100).toInt()}% confidence",
            type = NotificationType.AI_SIGNAL,
            priority = if (confidence > 0.8f) NotificationPriority.HIGH else NotificationPriority.MEDIUM,
            timestamp = System.currentTimeMillis(),
            metadata = mapOf(
                "symbol" to symbol,
                "signal" to signal,
                "confidence" to confidence.toString()
            )
        )
        
        return createNotification(notification)
    }
    
    override suspend fun createConnectionNotification(
        isConnected: Boolean,
        message: String
    ): Result<Unit> {
        val notification = Notification(
            id = System.currentTimeMillis().toString(),
            title = if (isConnected) "Connection Restored" else "Connection Lost",
            message = message,
            type = if (isConnected) NotificationType.CONNECTION_RESTORED else NotificationType.CONNECTION_LOST,
            priority = if (isConnected) NotificationPriority.LOW else NotificationPriority.HIGH,
            timestamp = System.currentTimeMillis()
        )
        
        return createNotification(notification)
    }
    
    private fun NotificationEntity.toDomainModel(): Notification {
        return Notification(
            id = id,
            title = title,
            message = message,
            type = type,
            priority = priority,
            timestamp = timestamp,
            isRead = isRead,
            actionUrl = actionUrl,
            metadata = try {
                json.decodeFromString<Map<String, String>>(metadata)
            } catch (e: Exception) {
                emptyMap()
            }
        )
    }
}