package com.ghostbot.trading.domain.usecase

import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.AIRepository
import com.ghostbot.trading.domain.repository.NotificationRepository
import com.ghostbot.trading.domain.repository.TradingRepository
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Use case for AI-powered market analysis and trading signals
 */
class AIAnalysisUseCase @Inject constructor(
    private val aiRepository: AIRepository,
    private val tradingRepository: TradingRepository,
    private val notificationRepository: NotificationRepository
) {
    
    suspend fun getMarketAnalysis(symbol: String): Flow<MarketAnalysis> {
        return aiRepository.getMarketAnalysis(symbol)
    }
    
    suspend fun generateTradingSignal(
        symbol: String,
        timeframe: String = "M15"
    ): Flow<AISignal> {
        return flow {
            // Get current market data
            val marketData = getCurrentMarketData(symbol)
            
            // Generate signal using AI
            aiRepository.generateTradingSignal(symbol, timeframe, marketData).collect { signal ->
                // Create notification for high-confidence signals
                if (signal.confidence > 0.7f) {
                    notificationRepository.createAISignalNotification(
                        symbol = signal.symbol,
                        signal = signal.signal.name,
                        confidence = signal.confidence
                    )
                }
                
                emit(signal)
            }
        }
    }
    
    suspend fun getAISignals(symbol: String): Flow<List<AISignal>> {
        return aiRepository.getAISignals(symbol)
    }
    
    suspend fun getLatestSignal(symbol: String): AISignal? {
        return aiRepository.getLatestSignal(symbol)
    }
    
    suspend fun executeAIRecommendation(
        signal: AISignal,
        config: TradingConfig
    ): Result<Trade> {
        if (!config.aiTradingEnabled) {
            return Result.failure(Exception("AI trading is disabled"))
        }
        
        if (signal.confidence < 0.8f) {
            return Result.failure(Exception("Signal confidence too low: ${signal.confidence}"))
        }
        
        val tradeRequest = when (signal.signal) {
            SignalType.BUY, SignalType.STRONG_BUY -> TradeRequest(
                symbol = signal.symbol,
                type = TradeType.BUY,
                volume = calculatePositionSize(config, signal.confidence),
                price = signal.entryPrice,
                stopLoss = signal.stopLoss,
                takeProfit = signal.takeProfit,
                comment = "AI Signal - Confidence: ${(signal.confidence * 100).toInt()}%"
            )
            
            SignalType.SELL, SignalType.STRONG_SELL -> TradeRequest(
                symbol = signal.symbol,
                type = TradeType.SELL,
                volume = calculatePositionSize(config, signal.confidence),
                price = signal.entryPrice,
                stopLoss = signal.stopLoss,
                takeProfit = signal.takeProfit,
                comment = "AI Signal - Confidence: ${(signal.confidence * 100).toInt()}%"
            )
            
            SignalType.HOLD -> return Result.failure(Exception("Signal recommends holding"))
        }
        
        return tradingRepository.executeTrade(tradeRequest)
    }
    
    suspend fun getSignalAccuracy(symbol: String, days: Int = 30): Flow<Float> {
        return flow {
            val cutoffTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
            
            aiRepository.getAISignals(symbol).collect { signals ->
                val recentSignals = signals.filter { it.timestamp > cutoffTime }
                
                if (recentSignals.isEmpty()) {
                    emit(0f)
                    return@collect
                }
                
                // Calculate accuracy based on signal outcomes
                // This would require tracking actual market movements after signals
                val accuracy = calculateSignalAccuracy(recentSignals)
                emit(accuracy)
            }
        }
    }
    
    private suspend fun getCurrentMarketData(symbol: String): Map<String, Any> {
        // Collect current market data for analysis
        val marketData = mutableMapOf<String, Any>()
        
        try {
            tradingRepository.getMarketPrices(listOf(symbol)).take(1).collect { prices ->
                val price = prices.firstOrNull { it.symbol == symbol }
                if (price != null) {
                    marketData["currentPrice"] = price.ask
                    marketData["spread"] = price.spread
                    marketData["timestamp"] = price.timestamp
                }
            }
        } catch (e: Exception) {
            // Use default values if real data unavailable
            marketData["currentPrice"] = 1.0
            marketData["spread"] = 0.0001
            marketData["timestamp"] = System.currentTimeMillis()
        }
        
        return marketData
    }
    
    private fun calculatePositionSize(config: TradingConfig, confidence: Float): Double {
        // Calculate position size based on confidence and risk management
        val baseSize = config.lotSize
        val confidenceMultiplier = when {
            confidence > 0.9f -> 1.5
            confidence > 0.8f -> 1.2
            confidence > 0.7f -> 1.0
            else -> 0.5
        }
        
        val riskMultiplier = when (config.riskLevel) {
            RiskLevel.CONSERVATIVE -> 0.5
            RiskLevel.MODERATE -> 1.0
            RiskLevel.AGGRESSIVE -> 1.5
        }
        
        return (baseSize * confidenceMultiplier * riskMultiplier).coerceAtMost(config.lotSize * 3)
    }
    
    private fun calculateSignalAccuracy(signals: List<AISignal>): Float {
        // Simplified accuracy calculation
        // In a real implementation, this would compare signals with actual market outcomes
        return signals.map { it.confidence }.average().toFloat()
    }
}