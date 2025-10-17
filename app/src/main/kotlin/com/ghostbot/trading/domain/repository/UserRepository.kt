package com.ghostbot.trading.domain.repository

import com.ghostbot.trading.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user management and authentication
 */
interface UserRepository {
    
    // Authentication
    suspend fun login(credentials: LoginCredentials): Result<User>
    suspend fun logout(): Result<Unit>
    
    // PIN Management
    suspend fun verifyPin(pin: String): Result<Boolean>
    suspend fun setPin(pin: String): Result<Unit>
    
    // Settings
    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>
    suspend fun saveApiKeys(apiKeys: ApiKeys): Result<Unit>
    suspend fun saveTradingConfig(config: TradingConfig): Result<Unit>
    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit>
    
    // User Data
    suspend fun getCurrentUser(): Flow<User?>
    
    // Security
    suspend fun isPinSet(): Boolean
    suspend fun isLockedOut(): Boolean
    suspend fun getFailedAttempts(): Int
    suspend fun getLockoutEndTime(): Long
}