package com.ghostbot.trading.data.repository

import com.ghostbot.trading.data.security.SecureStorage
import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.UserRepository
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val secureStorage: SecureStorage
) : UserRepository {
    
    private val _user = MutableStateFlow<User?>(null)
    
    override suspend fun login(credentials: LoginCredentials): Result<User> {
        return try {
            // Store credentials securely
            secureStorage.saveLoginCredentials(credentials)
            secureStorage.saveLastLoginTime()
            secureStorage.clearFailedAttempts()
            
            // Create user object
            val user = User(
                id = credentials.loginId,
                pin = "", // PIN is handled separately
                biometricEnabled = secureStorage.isBiometricEnabled(),
                accounts = emptyList(), // Will be populated after connecting to API
                activeAccountId = null,
                tradingConfig = getDefaultTradingConfig(),
                notificationSettings = getDefaultNotificationSettings(),
                apiKeys = secureStorage.getApiKeys() ?: ApiKeys(),
                securitySettings = getDefaultSecuritySettings(),
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis()
            )
            
            _user.value = user
            Result.success(user)
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            secureStorage.clearLoginCredentials()
            secureStorage.clearAuthToken()
            _user.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            Result.failure(e)
        }
    }
    
    override suspend fun verifyPin(pin: String): Result<Boolean> {
        return try {
            val isValid = secureStorage.verifyPin(pin)
            if (isValid) {
                secureStorage.clearFailedAttempts()
                Result.success(true)
            } else {
                val attempts = secureStorage.getFailedAttempts() + 1
                secureStorage.saveFailedAttempts(attempts)
                
                if (attempts >= 3) {
                    val lockoutEnd = System.currentTimeMillis() + 30_000 // 30 seconds
                    secureStorage.saveLockoutEndTime(lockoutEnd)
                }
                
                Result.success(false)
            }
        } catch (e: Exception) {
            Timber.e(e, "PIN verification failed")
            Result.failure(e)
        }
    }
    
    override suspend fun setPin(pin: String): Result<Unit> {
        return try {
            secureStorage.savePin(pin)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set PIN")
            Result.failure(e)
        }
    }
    
    override suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> {
        return try {
            secureStorage.setBiometricEnabled(enabled)
            _user.value = _user.value?.copy(biometricEnabled = enabled)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to set biometric setting")
            Result.failure(e)
        }
    }
    
    override suspend fun saveApiKeys(apiKeys: ApiKeys): Result<Unit> {
        return try {
            secureStorage.saveApiKeys(apiKeys)
            _user.value = _user.value?.copy(apiKeys = apiKeys)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save API keys")
            Result.failure(e)
        }
    }
    
    override suspend fun saveTradingConfig(config: TradingConfig): Result<Unit> {
        return try {
            _user.value = _user.value?.copy(tradingConfig = config)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save trading config")
            Result.failure(e)
        }
    }
    
    override suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> {
        return try {
            _user.value = _user.value?.copy(notificationSettings = settings)
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save notification settings")
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUser(): Flow<User?> {
        return _user.asStateFlow()
    }
    
    override suspend fun isPinSet(): Boolean {
        return secureStorage.isPinSet()
    }
    
    override suspend fun isLockedOut(): Boolean {
        return secureStorage.isLockedOut()
    }
    
    override suspend fun getFailedAttempts(): Int {
        return secureStorage.getFailedAttempts()
    }
    
    override suspend fun getLockoutEndTime(): Long {
        return secureStorage.getLockoutEndTime()
    }
    
    private fun getDefaultTradingConfig(): TradingConfig {
        return TradingConfig(
            tradingHours = TradingHours()
        )
    }
    
    private fun getDefaultNotificationSettings(): NotificationSettings {
        return NotificationSettings()
    }
    
    private fun getDefaultSecuritySettings(): SecuritySettings {
        return SecuritySettings()
    }
}