package com.ghostbot.trading.data.network

import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Handles retry logic with exponential backoff and jitter
 */
class RetryHandler {
    
    companion object {
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_BASE_DELAY_MS = 1000L
        private const val DEFAULT_MAX_DELAY_MS = 30_000L
        private const val DEFAULT_BACKOFF_MULTIPLIER = 2.0
        private const val DEFAULT_JITTER_FACTOR = 0.1
    }
    
    /**
     * Execute operation with retry logic
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = DEFAULT_MAX_RETRIES,
        baseDelayMs: Long = DEFAULT_BASE_DELAY_MS,
        maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
        backoffMultiplier: Double = DEFAULT_BACKOFF_MULTIPLIER,
        jitterFactor: Double = DEFAULT_JITTER_FACTOR,
        shouldRetry: (Throwable) -> Boolean = { true },
        operation: suspend () -> T
    ): T {
        var lastException: Throwable? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Throwable) {
                lastException = e
                
                // Don't retry on the last attempt
                if (attempt == maxRetries) {
                    throw e
                }
                
                // Check if we should retry this exception
                if (!shouldRetry(e)) {
                    throw e
                }
                
                // Calculate delay with exponential backoff and jitter
                val exponentialDelay = (baseDelayMs * backoffMultiplier.pow(attempt.toDouble())).toLong()
                val delayWithMax = min(exponentialDelay, maxDelayMs)
                val jitter = (delayWithMax * jitterFactor * Random.nextDouble()).toLong()
                val finalDelay = delayWithMax + jitter
                
                Timber.d("Retry attempt ${attempt + 1}/$maxRetries after ${finalDelay}ms delay. Error: ${e.message}")
                
                delay(finalDelay)
            }
        }
        
        // This should never be reached, but just in case
        throw lastException ?: Exception("Unknown error during retry execution")
    }
    
    /**
     * Predefined retry policies for common scenarios
     */
    object RetryPolicies {
        
        /**
         * Network-related errors (timeouts, connection issues)
         */
        val networkErrors: (Throwable) -> Boolean = { exception ->
            when (exception) {
                is java.net.SocketTimeoutException,
                is java.net.ConnectException,
                is java.net.UnknownHostException,
                is javax.net.ssl.SSLException -> true
                else -> false
            }
        }
        
        /**
         * HTTP 5xx server errors (but not 4xx client errors)
         */
        val serverErrors: (Throwable) -> Boolean = { exception ->
            when {
                exception.message?.contains("HTTP 5") == true -> true
                exception.message?.contains("Internal Server Error") == true -> true
                exception.message?.contains("Service Unavailable") == true -> true
                exception.message?.contains("Gateway Timeout") == true -> true
                else -> false
            }
        }
        
        /**
         * WebSocket connection errors
         */
        val webSocketErrors: (Throwable) -> Boolean = { exception ->
            when {
                exception.message?.contains("WebSocket") == true -> true
                exception.message?.contains("Connection reset") == true -> true
                exception.message?.contains("Connection refused") == true -> true
                else -> networkErrors(exception)
            }
        }
        
        /**
         * API rate limiting errors
         */
        val rateLimitErrors: (Throwable) -> Boolean = { exception ->
            when {
                exception.message?.contains("429") == true -> true
                exception.message?.contains("Rate limit") == true -> true
                exception.message?.contains("Too many requests") == true -> true
                else -> false
            }
        }
        
        /**
         * Combined policy for API operations
         */
        val apiOperations: (Throwable) -> Boolean = { exception ->
            networkErrors(exception) || serverErrors(exception) || rateLimitErrors(exception)
        }
        
        /**
         * Never retry (for operations that should fail immediately)
         */
        val noRetry: (Throwable) -> Boolean = { false }
        
        /**
         * Retry everything (for testing purposes)
         */
        val retryAll: (Throwable) -> Boolean = { true }
    }
    
    /**
     * Predefined retry configurations
     */
    object RetryConfigs {
        
        /**
         * Fast retry for quick operations
         */
        suspend fun <T> fastRetry(
            operation: suspend () -> T,
            shouldRetry: (Throwable) -> Boolean = RetryPolicies.apiOperations
        ): T {
            return RetryHandler().executeWithRetry(
                maxRetries = 2,
                baseDelayMs = 500L,
                maxDelayMs = 5_000L,
                shouldRetry = shouldRetry,
                operation = operation
            )
        }
        
        /**
         * Standard retry for most operations
         */
        suspend fun <T> standardRetry(
            operation: suspend () -> T,
            shouldRetry: (Throwable) -> Boolean = RetryPolicies.apiOperations
        ): T {
            return RetryHandler().executeWithRetry(
                maxRetries = 3,
                baseDelayMs = 1_000L,
                maxDelayMs = 15_000L,
                shouldRetry = shouldRetry,
                operation = operation
            )
        }
        
        /**
         * Aggressive retry for critical operations
         */
        suspend fun <T> aggressiveRetry(
            operation: suspend () -> T,
            shouldRetry: (Throwable) -> Boolean = RetryPolicies.apiOperations
        ): T {
            return RetryHandler().executeWithRetry(
                maxRetries = 5,
                baseDelayMs = 2_000L,
                maxDelayMs = 60_000L,
                backoffMultiplier = 2.5,
                shouldRetry = shouldRetry,
                operation = operation
            )
        }
        
        /**
         * WebSocket connection retry
         */
        suspend fun <T> webSocketRetry(
            operation: suspend () -> T
        ): T {
            return RetryHandler().executeWithRetry(
                maxRetries = 5,
                baseDelayMs = 1_000L,
                maxDelayMs = 30_000L,
                shouldRetry = RetryPolicies.webSocketErrors,
                operation = operation
            )
        }
        
        /**
         * Rate limit aware retry
         */
        suspend fun <T> rateLimitRetry(
            operation: suspend () -> T
        ): T {
            return RetryHandler().executeWithRetry(
                maxRetries = 3,
                baseDelayMs = 5_000L, // Longer initial delay for rate limits
                maxDelayMs = 120_000L, // Up to 2 minutes for rate limits
                backoffMultiplier = 3.0, // More aggressive backoff
                shouldRetry = RetryPolicies.rateLimitErrors,
                operation = operation
            )
        }
    }
}

/**
 * Extension functions for easier usage
 */
suspend fun <T> retryOnFailure(
    maxRetries: Int = 3,
    shouldRetry: (Throwable) -> Boolean = RetryHandler.RetryPolicies.apiOperations,
    operation: suspend () -> T
): T {
    return RetryHandler().executeWithRetry(
        maxRetries = maxRetries,
        shouldRetry = shouldRetry,
        operation = operation
    )
}

suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    baseDelayMs: Long = 1000L,
    maxDelayMs: Long = 30_000L,
    operation: suspend () -> T
): T {
    return RetryHandler().executeWithRetry(
        maxRetries = maxRetries,
        baseDelayMs = baseDelayMs,
        maxDelayMs = maxDelayMs,
        operation = operation
    )
}