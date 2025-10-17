package com.ghostbot.trading.domain.repository

import com.ghostbot.trading.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for AI-powered trading analysis
 */
interface AIRepository {
    
    // Market Analysis
    suspend fun getMarketAnalysis(symbol: String): Flow<MarketAnalysis>
    
    // AI Signals
    suspend fun generateTradingSignal(
        symbol: String,
        timeframe: String,
        marketData: Map<String, Any>
    ): Flow<AISignal>
    
    suspend fun getAISignals(symbol: String): Flow<List<AISignal>>
    suspend fun getLatestSignal(symbol: String): AISignal?
}