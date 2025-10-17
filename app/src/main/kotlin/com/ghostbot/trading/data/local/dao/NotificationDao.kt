package com.ghostbot.trading.data.local.dao

import androidx.room.*
import com.ghostbot.trading.data.local.entity.NotificationEntity
import com.ghostbot.trading.domain.model.NotificationPriority
import com.ghostbot.trading.domain.model.NotificationType
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE type = :type ORDER BY timestamp DESC")
    fun getNotificationsByType(type: NotificationType): Flow<List<NotificationEntity>>
    
    @Query("SELECT * FROM notifications WHERE priority = :priority ORDER BY timestamp DESC")
    fun getNotificationsByPriority(priority: NotificationPriority): Flow<List<NotificationEntity>>
    
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)
    
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markAsRead(notificationId: String)
    
    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
    
    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)
    
    @Query("DELETE FROM notifications")
    suspend fun deleteAllNotifications()
    
    @Query("DELETE FROM notifications WHERE timestamp < :cutoffTime")
    suspend fun deleteOldNotifications(cutoffTime: Long)
}