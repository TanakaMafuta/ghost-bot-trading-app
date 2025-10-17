package com.ghostbot.trading.data.remote.websocket

import com.ghostbot.trading.data.remote.dto.websocket.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * WebSocket client interface for real-time Deriv API communication
 */
interface DerivWebSocketClient {
    
    val connectionState: StateFlow<WebSocketState>
    val messages: Flow<WebSocketMessage>
    
    suspend fun connect(url: String, appId: String)
    suspend fun disconnect()
    suspend fun authorize(token: String): Boolean
    
    // Subscription methods
    suspend fun subscribeTicks(symbols: List<String>)
    suspend fun subscribeCandles(symbol: String, granularity: Int = 60)
    suspend fun subscribePortfolio()
    suspend fun subscribeBalance()
    suspend fun subscribeProposalOpenContract()
    
    // Unsubscription methods
    suspend fun unsubscribeTicks(symbols: List<String>)
    suspend fun unsubscribeCandles(symbol: String)
    suspend fun unsubscribePortfolio()
    suspend fun unsubscribeBalance()
    
    // Trading operations
    suspend fun buy(proposal: BuyProposal): TradeResult
    suspend fun sell(contractId: String, price: Double): TradeResult
    
    // Account operations
    suspend fun getAccountStatus(): AccountStatus
    suspend fun getPortfolio(): Portfolio
    suspend fun getBalance(): Balance
    
    // Market data
    suspend fun getActiveSymbols(): List<Symbol>
    suspend fun getTicks(symbol: String, count: Int = 1000): TicksHistory
    
    fun isConnected(): Boolean
    fun isAuthorized(): Boolean
}

enum class WebSocketState {
    DISCONNECTED, CONNECTING, CONNECTED, AUTHORIZED, ERROR
}

sealed class WebSocketMessage {
    data class Tick(val data: TickData) : WebSocketMessage()
    data class Candle(val data: CandleData) : WebSocketMessage()
    data class PortfolioUpdate(val data: PortfolioData) : WebSocketMessage()
    data class BalanceUpdate(val data: BalanceData) : WebSocketMessage()
    data class TradeUpdate(val data: TradeData) : WebSocketMessage()
    data class Error(val message: String, val code: String? = null) : WebSocketMessage()
    data class Connected(val message: String) : WebSocketMessage()
    data class Disconnected(val reason: String) : WebSocketMessage()
}