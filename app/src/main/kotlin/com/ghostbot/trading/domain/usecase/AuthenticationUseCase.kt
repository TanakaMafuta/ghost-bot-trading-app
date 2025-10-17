package com.ghostbot.trading.domain.usecase

import com.ghostbot.trading.domain.model.LoginCredentials
import com.ghostbot.trading.domain.model.User
import com.ghostbot.trading.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for user authentication and security
 */
class AuthenticationUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    
    suspend fun verifyPin(pin: String): Result<Boolean> {
        if (userRepository.isLockedOut()) {
            return Result.failure(Exception("Account is locked out"))
        }
        
        return userRepository.verifyPin(pin)
    }
    
    suspend fun setPin(pin: String): Result<Unit> {
        if (pin.length != 8 || !pin.all { it.isDigit() }) {
            return Result.failure(Exception("PIN must be exactly 8 digits"))
        }
        
        return userRepository.setPin(pin)
    }
    
    suspend fun login(credentials: LoginCredentials): Result<User> {
        if (credentials.loginId.isBlank() || credentials.password.isBlank()) {
            return Result.failure(Exception("Login ID and password are required"))
        }
        
        return userRepository.login(credentials)
    }
    
    suspend fun logout(): Result<Unit> {
        return userRepository.logout()
    }
    
    suspend fun getCurrentUser(): Flow<User?> {
        return userRepository.getCurrentUser()
    }
    
    suspend fun isPinSet(): Boolean {
        return userRepository.isPinSet()
    }
    
    suspend fun isLockedOut(): Boolean {
        return userRepository.isLockedOut()
    }
    
    suspend fun getFailedAttempts(): Int {
        return userRepository.getFailedAttempts()
    }
    
    suspend fun getLockoutEndTime(): Long {
        return userRepository.getLockoutEndTime()
    }
    
    suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> {
        return userRepository.setBiometricEnabled(enabled)
    }
}