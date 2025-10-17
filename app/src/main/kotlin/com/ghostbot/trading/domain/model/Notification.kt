package com.ghostbot.trading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val priority: NotificationPriority,
    val timestamp: Long,
    val isRead: Boolean = false,
    val actionUrl: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class NotificationType {
    TRADE_EXECUTED,
    AI_SIGNAL,
    MARGIN_CALL,
    CONNECTION_LOST,
    CONNECTION_RESTORED,
    BALANCE_UPDATE,
    NEWS_ALERT,
    SYSTEM_MESSAGE,
    ERROR,
    WARNING
}

@Serializable
enum class NotificationPriority {
    LOW, MEDIUM, HIGH, CRITICAL
}

@Serializable
data class NotificationSettings(
    val tradeAlerts: Boolean = true,
    val aiInsights: Boolean = true,
    val marketNews: Boolean = true,
    val systemAlerts: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHours: QuietHours? = null
)

@Serializable
data class QuietHours(
    val startHour: Int,
    val endHour: Int,
    val enabled: Boolean = true
)