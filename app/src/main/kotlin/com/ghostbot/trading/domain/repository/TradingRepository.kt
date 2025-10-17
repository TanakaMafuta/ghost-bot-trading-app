package com.ghostbot.trading.domain.repository

import com.ghostbot.trading.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for trading operations
 * Abstracts data layer for clean architecture
 */
interface TradingRepository {
    
    // Account Management
    suspend fun getAccount(): Flow<Account?>
    suspend fun getBalance(): Flow<Double>
    
    // Trade Management
    suspend fun getTrades(accountId: String): Flow<List<Trade>>
    suspend fun getOpenTrades(accountId: String): Flow<List<Trade>>
    suspend fun executeTrade(request: TradeRequest): Result<Trade>
    suspend fun closeTrade(tradeId: String): Result<Trade>
    
    // Market Data
    suspend fun getMarketPrices(symbols: List<String>): Flow<List<MarketPrice>>
    suspend fun getSymbols(): Flow<List<Symbol>>
}