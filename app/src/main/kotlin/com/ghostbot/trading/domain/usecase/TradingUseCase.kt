package com.ghostbot.trading.domain.usecase

import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.NotificationRepository
import com.ghostbot.trading.domain.repository.TradingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for trading operations with business logic
 */
class TradingUseCase @Inject constructor(
    private val tradingRepository: TradingRepository,
    private val notificationRepository: NotificationRepository
) {
    
    suspend fun getAccount(): Flow<Account?> {
        return tradingRepository.getAccount()
    }
    
    suspend fun getBalance(): Flow<Double> {
        return tradingRepository.getBalance()
    }
    
    suspend fun getTrades(accountId: String): Flow<List<Trade>> {
        return tradingRepository.getTrades(accountId)
    }
    
    suspend fun getOpenTrades(accountId: String): Flow<List<Trade>> {
        return tradingRepository.getOpenTrades(accountId)
    }
    
    suspend fun getTradingMetrics(accountId: String): Flow<TradingMetrics> {
        return tradingRepository.getTrades(accountId).map { trades ->
            calculateMetrics(trades)
        }
    }
    
    suspend fun executeTrade(
        request: TradeRequest,
        config: TradingConfig
    ): Result<Trade> {
        // Validate trade request
        val validation = validateTradeRequest(request, config)
        if (validation.isFailure) {
            return validation
        }
        
        // Execute trade
        val result = tradingRepository.executeTrade(request)
        
        // Create notification
        result.onSuccess { trade ->
            val message = "${trade.type} ${trade.volume} ${trade.symbol} at ${trade.openPrice}"
            notificationRepository.createTradeNotification(
                message = message,
                tradeId = trade.id,
                profit = trade.profit
            )
        }
        
        return result
    }
    
    suspend fun closeTrade(tradeId: String): Result<Trade> {
        val result = tradingRepository.closeTrade(tradeId)
        
        // Create notification
        result.onSuccess { trade ->
            val message = "Trade ${trade.id} closed. Profit: ${trade.profit}"
            notificationRepository.createTradeNotification(
                message = message,
                tradeId = trade.id,
                profit = trade.profit
            )
        }
        
        return result
    }
    
    suspend fun getMarketPrices(symbols: List<String>): Flow<List<MarketPrice>> {
        return tradingRepository.getMarketPrices(symbols)
    }
    
    suspend fun getSymbols(): Flow<List<Symbol>> {
        return tradingRepository.getSymbols()
    }
    
    private fun validateTradeRequest(
        request: TradeRequest,
        config: TradingConfig
    ): Result<Trade> {
        // Check volume limits
        if (request.volume < 0.01 || request.volume > 100.0) {
            return Result.failure(Exception("Invalid volume: ${request.volume}"))
        }
        
        // Check if symbol is allowed
        if (config.allowedSymbols.isNotEmpty() && !config.allowedSymbols.contains(request.symbol)) {
            return Result.failure(Exception("Symbol ${request.symbol} is not allowed"))
        }
        
        // Check maximum lot size
        if (request.volume > config.lotSize * 10) {
            return Result.failure(Exception("Volume exceeds maximum allowed"))
        }
        
        return Result.success(
            Trade(
                id = "",
                symbol = request.symbol,
                type = request.type,
                volume = request.volume,
                openPrice = request.price ?: 0.0,
                stopLoss = request.stopLoss,
                takeProfit = request.takeProfit,
                openTime = System.currentTimeMillis(),
                profit = 0.0,
                status = TradeStatus.PENDING,
                comment = request.comment
            )
        )
    }
    
    private fun calculateMetrics(trades: List<Trade>): TradingMetrics {
        val closedTrades = trades.filter { it.status == TradeStatus.CLOSED }
        val winningTrades = closedTrades.filter { it.profit > 0 }
        val losingTrades = closedTrades.filter { it.profit < 0 }
        
        val totalProfit = closedTrades.sumOf { it.profit }
        val totalLoss = losingTrades.sumOf { kotlin.math.abs(it.profit) }
        val winRate = if (closedTrades.isNotEmpty()) {
            winningTrades.size.toFloat() / closedTrades.size.toFloat()
        } else 0f
        
        val profitFactor = if (totalLoss > 0) totalProfit / totalLoss else 0.0
        
        return TradingMetrics(
            totalTrades = closedTrades.size,
            winningTrades = winningTrades.size,
            losingTrades = losingTrades.size,
            winRate = winRate,
            totalProfit = totalProfit,
            profitFactor = profitFactor,
            openTrades = trades.count { it.status == TradeStatus.OPEN }
        )
    }
}

data class TradingMetrics(
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Float,
    val totalProfit: Double,
    val profitFactor: Double,
    val openTrades: Int
)