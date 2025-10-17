package com.ghostbot.trading.data.repository

import com.ghostbot.trading.data.local.dao.TradeDao
import com.ghostbot.trading.data.local.entity.TradeEntity
import com.ghostbot.trading.data.remote.api.DerivApiService
import com.ghostbot.trading.data.remote.websocket.DerivWebSocketClient
import com.ghostbot.trading.data.security.SecureStorage
import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.TradingRepository
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradingRepositoryImpl @Inject constructor(
    private val derivApiService: DerivApiService,
    private val webSocketClient: DerivWebSocketClient,
    private val tradeDao: TradeDao,
    private val secureStorage: SecureStorage
) : TradingRepository {
    
    override suspend fun getAccount(): Flow<Account?> = flow {
        try {
            val token = secureStorage.getAuthToken()?.token ?: throw IllegalStateException("Not authenticated")
            val response = derivApiService.getAccountStatus("Bearer $token")
            
            if (response.isSuccessful) {
                val accountData = response.body()?.account_status
                accountData?.let {
                    emit(Account(
                        accountId = token, // Use token as temp ID
                        accountType = AccountType.DEMO, // Determine from data
                        balance = 0.0, // Get from balance API
                        equity = 0.0,
                        margin = 0.0,
                        freeMargin = 0.0,
                        marginLevel = 0.0,
                        currency = "USD", // Default currency
                        leverage = 100,
                        server = "Deriv-Demo",
                        isConnected = webSocketClient.isConnected()
                    ))
                }
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get account")
            emit(null)
        }
    }
    
    override suspend fun getBalance(): Flow<Double> = flow {
        try {
            if (webSocketClient.isConnected()) {
                webSocketClient.subscribeBalance()
                webSocketClient.messages.collect { message ->
                    if (message is com.ghostbot.trading.data.remote.websocket.WebSocketMessage.BalanceUpdate) {
                        emit(message.data.balance.balance)
                    }
                }
            } else {
                // Fallback to REST API
                emit(0.0)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get balance")
            emit(0.0)
        }
    }
    
    override suspend fun getTrades(accountId: String): Flow<List<Trade>> {
        return tradeDao.getTradesByAccount(accountId).map { entities ->
            entities.map { entity ->
                Trade(
                    id = entity.id,
                    symbol = entity.symbol,
                    type = entity.type,
                    volume = entity.volume,
                    openPrice = entity.openPrice,
                    closePrice = entity.closePrice,
                    stopLoss = entity.stopLoss,
                    takeProfit = entity.takeProfit,
                    openTime = entity.openTime,
                    closeTime = entity.closeTime,
                    profit = entity.profit,
                    commission = entity.commission,
                    swap = entity.swap,
                    status = entity.status,
                    comment = entity.comment
                )
            }
        }
    }
    
    override suspend fun getOpenTrades(accountId: String): Flow<List<Trade>> {
        return tradeDao.getTradesByStatus(accountId, TradeStatus.OPEN).map { entities ->
            entities.map { entity ->
                Trade(
                    id = entity.id,
                    symbol = entity.symbol,
                    type = entity.type,
                    volume = entity.volume,
                    openPrice = entity.openPrice,
                    closePrice = entity.closePrice,
                    stopLoss = entity.stopLoss,
                    takeProfit = entity.takeProfit,
                    openTime = entity.openTime,
                    closeTime = entity.closeTime,
                    profit = entity.profit,
                    commission = entity.commission,
                    swap = entity.swap,
                    status = entity.status,
                    comment = entity.comment
                )
            }
        }
    }
    
    override suspend fun executeTrade(request: TradeRequest): Result<Trade> {
        return try {
            val token = secureStorage.getAuthToken()?.token ?: throw IllegalStateException("Not authenticated")
            
            // First get a proposal
            val proposalRequest = com.ghostbot.trading.data.remote.dto.ProposalRequest(
                amount = request.volume,
                contract_type = when (request.type) {
                    TradeType.BUY -> "CALL"
                    TradeType.SELL -> "PUT"
                },
                currency = "USD",
                duration = 5,
                duration_unit = "m",
                symbol = request.symbol
            )
            
            val proposalResponse = derivApiService.getProposal(proposalRequest)
            
            if (proposalResponse.isSuccessful) {
                val proposal = proposalResponse.body()?.proposal
                if (proposal != null) {
                    // Execute the trade via WebSocket for better real-time handling
                    if (webSocketClient.isConnected()) {
                        val buyProposal = com.ghostbot.trading.data.remote.dto.websocket.BuyProposal(
                            id = proposal.id,
                            price = proposal.ask_price,
                            payout = proposal.payout,
                            symbol = request.symbol,
                            contractType = proposalRequest.contract_type
                        )
                        
                        val tradeResult = webSocketClient.buy(buyProposal)
                        when (tradeResult) {
                            is com.ghostbot.trading.data.remote.websocket.TradeResult.Success -> {
                                val trade = Trade(
                                    id = System.currentTimeMillis().toString(),
                                    symbol = request.symbol,
                                    type = request.type,
                                    volume = request.volume,
                                    openPrice = proposal.ask_price,
                                    stopLoss = request.stopLoss,
                                    takeProfit = request.takeProfit,
                                    openTime = System.currentTimeMillis(),
                                    profit = 0.0,
                                    status = TradeStatus.OPEN,
                                    comment = request.comment
                                )
                                
                                // Save to local database
                                val entity = TradeEntity(
                                    id = trade.id,
                                    symbol = trade.symbol,
                                    type = trade.type,
                                    volume = trade.volume,
                                    openPrice = trade.openPrice,
                                    stopLoss = trade.stopLoss,
                                    takeProfit = trade.takeProfit,
                                    openTime = trade.openTime,
                                    profit = trade.profit,
                                    status = trade.status,
                                    comment = trade.comment,
                                    accountId = secureStorage.getAuthToken()?.accountId ?: "default"
                                )
                                tradeDao.insertTrade(entity)
                                
                                Result.success(trade)
                            }
                            is com.ghostbot.trading.data.remote.websocket.TradeResult.Error -> {
                                Result.failure(Exception(tradeResult.message))
                            }
                        }
                    } else {
                        Result.failure(Exception("WebSocket not connected"))
                    }
                } else {
                    Result.failure(Exception("Failed to get proposal"))
                }
            } else {
                Result.failure(Exception("Proposal request failed: ${proposalResponse.message()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to execute trade")
            Result.failure(e)
        }
    }
    
    override suspend fun closeTrade(tradeId: String): Result<Trade> {
        return try {
            val trade = tradeDao.getTradeById(tradeId) ?: throw IllegalArgumentException("Trade not found")
            
            if (webSocketClient.isConnected()) {
                val sellResult = webSocketClient.sell(tradeId, 0.0) // Let market determine price
                when (sellResult) {
                    is com.ghostbot.trading.data.remote.websocket.TradeResult.Success -> {
                        val updatedTrade = trade.copy(
                            status = TradeStatus.CLOSED,
                            closeTime = System.currentTimeMillis(),
                            closePrice = 0.0 // Update with actual close price from response
                        )
                        
                        tradeDao.updateTrade(updatedTrade)
                        
                        Result.success(Trade(
                            id = updatedTrade.id,
                            symbol = updatedTrade.symbol,
                            type = updatedTrade.type,
                            volume = updatedTrade.volume,
                            openPrice = updatedTrade.openPrice,
                            closePrice = updatedTrade.closePrice,
                            stopLoss = updatedTrade.stopLoss,
                            takeProfit = updatedTrade.takeProfit,
                            openTime = updatedTrade.openTime,
                            closeTime = updatedTrade.closeTime,
                            profit = updatedTrade.profit,
                            commission = updatedTrade.commission,
                            swap = updatedTrade.swap,
                            status = updatedTrade.status,
                            comment = updatedTrade.comment
                        ))
                    }
                    is com.ghostbot.trading.data.remote.websocket.TradeResult.Error -> {
                        Result.failure(Exception(sellResult.message))
                    }
                }
            } else {
                Result.failure(Exception("WebSocket not connected"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to close trade")
            Result.failure(e)
        }
    }
    
    override suspend fun getMarketPrices(symbols: List<String>): Flow<List<MarketPrice>> = flow {
        try {
            if (webSocketClient.isConnected()) {
                webSocketClient.subscribeTicks(symbols)
                webSocketClient.messages.collect { message ->
                    if (message is com.ghostbot.trading.data.remote.websocket.WebSocketMessage.Tick) {
                        val tickData = message.data.tick
                        val marketPrice = MarketPrice(
                            symbol = tickData.symbol,
                            bid = tickData.bid,
                            ask = tickData.ask,
                            spread = tickData.ask - tickData.bid,
                            timestamp = tickData.epoch * 1000 // Convert to milliseconds
                        )
                        emit(listOf(marketPrice))
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get market prices")
            emit(emptyList())
        }
    }
    
    override suspend fun getSymbols(): Flow<List<Symbol>> = flow {
        try {
            val response = derivApiService.getActiveSymbols()
            if (response.isSuccessful) {
                val symbols = response.body()?.active_symbols?.map { symbolData ->
                    Symbol(
                        name = symbolData.symbol,
                        displayName = symbolData.display_name,
                        category = symbolData.market,
                        digits = 5, // Default
                        contractSize = 1.0,
                        tickSize = 0.00001,
                        tickValue = 1.0,
                        minVolume = 0.01,
                        maxVolume = 100.0,
                        stepVolume = 0.01,
                        isActive = symbolData.exchange_is_open == 1
                    )
                } ?: emptyList()
                
                emit(symbols)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get symbols")
            emit(emptyList())
        }
    }
}