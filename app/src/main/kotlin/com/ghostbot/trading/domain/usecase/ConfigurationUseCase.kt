package com.ghostbot.trading.domain.usecase

import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.UserRepository
import javax.inject.Inject

/**
 * Use case for managing trading configuration and settings
 */
class ConfigurationUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    
    suspend fun saveTradingConfig(config: TradingConfig): Result<Unit> {
        // Validate configuration
        val validation = validateTradingConfig(config)
        if (validation.isFailure) {
            return validation
        }
        
        return userRepository.saveTradingConfig(config)
    }
    
    suspend fun saveApiKeys(apiKeys: ApiKeys): Result<Unit> {
        // Validate API keys format
        val validation = validateApiKeys(apiKeys)
        if (validation.isFailure) {
            return validation
        }
        
        return userRepository.saveApiKeys(apiKeys)
    }
    
    suspend fun saveNotificationSettings(settings: NotificationSettings): Result<Unit> {
        return userRepository.saveNotificationSettings(settings)
    }
    
    suspend fun validateApiKey(apiKey: String, type: ApiKeyType): Result<Boolean> {
        return when (type) {
            ApiKeyType.DERIV -> validateDerivApiKey(apiKey)
            ApiKeyType.OPENAI -> validateOpenAIApiKey(apiKey)
            ApiKeyType.PERPLEXITY -> validatePerplexityApiKey(apiKey)
        }
    }
    
    private fun validateTradingConfig(config: TradingConfig): Result<Unit> {
        // Validate lot size
        if (config.lotSize <= 0 || config.lotSize > 100) {
            return Result.failure(Exception("Lot size must be between 0 and 100"))
        }
        
        // Validate leverage
        if (config.leverage < 1 || config.leverage > 1000) {
            return Result.failure(Exception("Leverage must be between 1 and 1000"))
        }
        
        // Validate risk percentages
        if (config.maxDrawdown < 0.01f || config.maxDrawdown > 1.0f) {
            return Result.failure(Exception("Max drawdown must be between 1% and 100%"))
        }
        
        if (config.stopLossPercent < 0.005f || config.stopLossPercent > 0.5f) {
            return Result.failure(Exception("Stop loss must be between 0.5% and 50%"))
        }
        
        if (config.takeProfitPercent < 0.01f || config.takeProfitPercent > 2.0f) {
            return Result.failure(Exception("Take profit must be between 1% and 200%"))
        }
        
        // Validate trading limits
        if (config.maxOpenTrades < 1 || config.maxOpenTrades > 50) {
            return Result.failure(Exception("Max open trades must be between 1 and 50"))
        }
        
        if (config.maxDailyTrades < 1 || config.maxDailyTrades > 200) {
            return Result.failure(Exception("Max daily trades must be between 1 and 200"))
        }
        
        // Validate trading hours
        val hours = config.tradingHours
        if (hours.startHour < 0 || hours.startHour > 23) {
            return Result.failure(Exception("Start hour must be between 0 and 23"))
        }
        
        if (hours.endHour < 0 || hours.endHour > 23) {
            return Result.failure(Exception("End hour must be between 0 and 23"))
        }
        
        return Result.success(Unit)
    }
    
    private fun validateApiKeys(apiKeys: ApiKeys): Result<Unit> {
        // Basic validation - in production, test actual connectivity
        apiKeys.derivApiKey?.let { key ->
            if (key.isBlank() || key.length < 10) {
                return Result.failure(Exception("Invalid Deriv API key format"))
            }
        }
        
        apiKeys.openaiApiKey?.let { key ->
            if (!key.startsWith("sk-") || key.length < 20) {
                return Result.failure(Exception("Invalid OpenAI API key format"))
            }
        }
        
        apiKeys.perplexityApiKey?.let { key ->
            if (key.isBlank() || key.length < 10) {
                return Result.failure(Exception("Invalid Perplexity API key format"))
            }
        }
        
        return Result.success(Unit)
    }
    
    private fun validateDerivApiKey(apiKey: String): Result<Boolean> {
        // In a real implementation, test connection to Deriv API
        return if (apiKey.isNotBlank() && apiKey.length >= 10) {
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid Deriv API key"))
        }
    }
    
    private fun validateOpenAIApiKey(apiKey: String): Result<Boolean> {
        return if (apiKey.startsWith("sk-") && apiKey.length >= 20) {
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid OpenAI API key format"))
        }
    }
    
    private fun validatePerplexityApiKey(apiKey: String): Result<Boolean> {
        return if (apiKey.isNotBlank() && apiKey.length >= 10) {
            Result.success(true)
        } else {
            Result.failure(Exception("Invalid Perplexity API key"))
        }
    }
}

enum class ApiKeyType {
    DERIV, OPENAI, PERPLEXITY
}