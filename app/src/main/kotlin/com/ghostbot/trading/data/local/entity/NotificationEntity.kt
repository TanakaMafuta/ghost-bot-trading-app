package com.ghostbot.trading.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghostbot.trading.domain.model.NotificationPriority
import com.ghostbot.trading.domain.model.NotificationType

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionUrl: String? = null,
    val metadata: String = "{}" // JSON string
)