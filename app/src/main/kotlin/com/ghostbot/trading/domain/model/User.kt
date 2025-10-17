package com.ghostbot.trading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val pin: String, // Encrypted
    val biometricEnabled: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val activeAccountId: String? = null,
    val tradingConfig: TradingConfig,
    val notificationSettings: NotificationSettings,
    val apiKeys: ApiKeys,
    val securitySettings: SecuritySettings,
    val createdAt: Long,
    val lastLoginAt: Long
)

@Serializable
data class ApiKeys(
    val derivApiKey: String? = null,
    val openaiApiKey: String? = null,
    val perplexityApiKey: String? = null,
    val newsApiKey: String? = null,
    val lastValidated: Long = 0L
)

@Serializable
data class SecuritySettings(
    val autoLockDuration: Long = 300_000L, // 5 minutes in milliseconds
    val maxFailedAttempts: Int = 3,
    val lockoutDuration: Long = 30_000L, // 30 seconds
    val enableSecureMode: Boolean = true, // Prevents screenshots
    val sessionTimeout: Long = 3600_000L // 1 hour
)

@Serializable
data class UserSession(
    val userId: String,
    val sessionId: String,
    val createdAt: Long,
    val expiresAt: Long,
    val isActive: Boolean = true
)