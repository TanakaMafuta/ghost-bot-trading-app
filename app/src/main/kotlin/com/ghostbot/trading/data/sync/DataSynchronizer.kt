package com.ghostbot.trading.data.sync

import com.ghostbot.trading.data.local.dao.*
import com.ghostbot.trading.data.local.entity.*
import com.ghostbot.trading.data.remote.websocket.DerivWebSocketManager
import com.ghostbot.trading.data.remote.websocket.AccountUpdate
import com.ghostbot.trading.data.remote.websocket.MarketDataUpdate
import com.ghostbot.trading.data.remote.websocket.TradeUpdate
import com.ghostbot.trading.data.security.SecureStorage
import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.NotificationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Synchronizes data between WebSocket streams, local database, and UI state
 */
@Singleton
class DataSynchronizer @Inject constructor(
    private val webSocketManager: DerivWebSocketManager,
    private val tradeDao: TradeDao,
    private val notificationDao: NotificationDao,
    private val aiSignalDao: AISignalDao,
    private val notificationRepository: NotificationRepository,
    private val secureStorage: SecureStorage
) {
    
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // State flows for UI consumption
    private val _accountBalance = MutableStateFlow(0.0)
    val accountBalance: StateFlow<Double> = _accountBalance.asStateFlow()
    
    private val _marketPrices = MutableStateFlow<Map<String, Double>>(emptyMap())
    val marketPrices: StateFlow<Map<String, Double>> = _marketPrices.asStateFlow()
    
    private val _openTrades = MutableStateFlow<List<Trade>>(emptyList())
    val openTrades: StateFlow<List<Trade>> = _openTrades.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow(false)
    val connectionStatus: StateFlow<Boolean> = _connectionStatus.asStateFlow()
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    private var syncJobs = mutableListOf<Job>()
    
    fun startSynchronization() {
        Timber.d("Starting data synchronization")
        _syncStatus.value = SyncStatus.SYNCING
        
        // Start all synchronization jobs
        syncJobs.clear()
        
        syncJobs.add(startConnectionStatusSync())
        syncJobs.add(startAccountUpdatesSync())
        syncJobs.add(startMarketDataSync())
        syncJobs.add(startTradeUpdatesSync())
        syncJobs.add(startNotificationCleanup())
        
        _syncStatus.value = SyncStatus.ACTIVE
    }
    
    fun stopSynchronization() {
        Timber.d("Stopping data synchronization")
        _syncStatus.value = SyncStatus.STOPPING
        
        syncJobs.forEach { it.cancel() }
        syncJobs.clear()
        
        _syncStatus.value = SyncStatus.IDLE
    }
    
    private fun startConnectionStatusSync(): Job {
        return syncScope.launch {
            webSocketManager.connectionState.collect { connectionState ->
                val isConnected = connectionState in listOf(
                    com.ghostbot.trading.data.remote.websocket.ConnectionState.CONNECTED,
                    com.ghostbot.trading.data.remote.websocket.ConnectionState.AUTHORIZED
                )
                
                if (_connectionStatus.value != isConnected) {
                    _connectionStatus.value = isConnected
                    
                    // Create connection status notification
                    notificationRepository.createConnectionNotification(
                        isConnected = isConnected,
                        message = if (isConnected) {
                            "Connected to trading servers"
                        } else {
                            "Disconnected from trading servers"
                        }
                    )
                }
            }
        }
    }
    
    private fun startAccountUpdatesSync(): Job {
        return syncScope.launch {
            webSocketManager.accountUpdates.collect { update ->
                when (update) {
                    is AccountUpdate.BalanceUpdate -> {
                        _accountBalance.value = update.balance
                        Timber.d("Balance updated: ${update.balance} ${update.currency}")
                        
                        // Create notification for significant balance changes
                        val previousBalance = _accountBalance.value
                        val changePercent = if (previousBalance > 0) {
                            ((update.balance - previousBalance) / previousBalance * 100)
                        } else 0.0
                        
                        if (kotlin.math.abs(changePercent) > 5.0) { // 5% change threshold
                            val changeDirection = if (changePercent > 0) "increased" else "decreased"
                            notificationRepository.createNotification(
                                Notification(
                                    id = System.currentTimeMillis().toString(),
                                    title = "Balance Update",
                                    message = "Account balance $changeDirection by ${
                                        String.format("%.1f", kotlin.math.abs(changePercent))
                                    }%",
                                    type = NotificationType.BALANCE_UPDATE,
                                    priority = NotificationPriority.MEDIUM,
                                    timestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }
                    
                    is AccountUpdate.PortfolioUpdate -> {
                        // Handle portfolio updates
                        Timber.d("Portfolio updated with ${update.contracts.size} contracts")
                    }
                }
            }
        }
    }
    
    private fun startMarketDataSync(): Job {
        return syncScope.launch {
            webSocketManager.marketData.collect { update ->
                when (update) {
                    is MarketDataUpdate.PriceUpdate -> {
                        val currentPrices = _marketPrices.value.toMutableMap()
                        currentPrices[update.symbol] = update.price
                        _marketPrices.value = currentPrices
                        
                        Timber.v("Price updated: ${update.symbol} = ${update.price}")
                    }
                    
                    is MarketDataUpdate.CandleUpdate -> {
                        // Handle candle updates
                        Timber.v("Candle updated: ${update.symbol} OHLC = ${update.open}/${update.high}/${update.low}/${update.close}")
                    }
                }
            }
        }
    }
    
    private fun startTradeUpdatesSync(): Job {
        return syncScope.launch {
            webSocketManager.tradeUpdates.collect { update ->
                try {
                    // Update local trade in database
                    val existingTrade = tradeDao.getTradeById(update.contractId)
                    
                    if (existingTrade != null) {
                        val updatedTrade = existingTrade.copy(
                            profit = update.profit,
                            closeTime = if (update.profit != 0.0) update.timestamp else null,
                            status = if (update.profit != 0.0) TradeStatus.CLOSED else TradeStatus.OPEN
                        )
                        
                        tradeDao.updateTrade(updatedTrade)
                        
                        // Update UI state
                        refreshOpenTrades()
                        
                        // Create trade update notification
                        val profitStatus = when {
                            update.profit > 0 -> "profit"
                            update.profit < 0 -> "loss"
                            else -> "update"
                        }
                        
                        notificationRepository.createTradeNotification(
                            message = "Trade ${update.contractId} $profitStatus: $${String.format("%.2f", update.profit)}",
                            tradeId = update.contractId,
                            profit = update.profit
                        )
                        
                        Timber.d("Trade updated: ${update.contractId} profit = ${update.profit}")
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to sync trade update")
                }
            }
        }
    }
    
    private fun startNotificationCleanup(): Job {
        return syncScope.launch {
            while (isActive) {
                try {
                    // Clean up old notifications (older than 30 days)
                    val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                    notificationDao.deleteOldNotifications(cutoffTime)
                    
                    // Clean up old AI signals (older than 7 days)
                    val signalCutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                    aiSignalDao.deleteOldSignals(signalCutoffTime)
                    
                    Timber.d("Cleaned up old notifications and signals")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to clean up old data")
                }
                
                // Run cleanup every 6 hours
                delay(6 * 60 * 60 * 1000L)
            }
        }
    }
    
    suspend fun refreshOpenTrades() {
        try {
            val accountId = secureStorage.getAuthToken()?.accountId ?: "default"
            tradeDao.getTradesByStatus(accountId, TradeStatus.OPEN).take(1).collect { tradeEntities ->
                val trades = tradeEntities.map { entity ->
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
                _openTrades.value = trades
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh open trades")
        }
    }
    
    suspend fun syncTradeHistory(accountId: String, forceRefresh: Boolean = false) {
        try {
            _syncStatus.value = SyncStatus.SYNCING
            
            // In a real implementation, this would fetch from the API
            // For now, we just refresh from local database
            refreshOpenTrades()
            
            _syncStatus.value = SyncStatus.ACTIVE
            Timber.d("Trade history synchronized for account $accountId")
        } catch (e: Exception) {
            Timber.e(e, "Failed to sync trade history")
            _syncStatus.value = SyncStatus.ERROR
        }
    }
    
    suspend fun handleEmergencyStop() {
        try {
            Timber.w("Emergency stop initiated")
            
            // Close all open trades
            val openTrades = _openTrades.value
            openTrades.forEach { trade ->
                try {
                    webSocketManager.sell(trade.id, 0.0) // Market close
                } catch (e: Exception) {
                    Timber.e(e, "Failed to emergency close trade ${trade.id}")
                }
            }
            
            // Create emergency notification
            notificationRepository.createNotification(
                Notification(
                    id = System.currentTimeMillis().toString(),
                    title = "Emergency Stop Executed",
                    message = "All open trades have been closed due to emergency stop",
                    type = NotificationType.SYSTEM_MESSAGE,
                    priority = NotificationPriority.CRITICAL,
                    timestamp = System.currentTimeMillis()
                )
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Emergency stop failed")
        }
    }
    
    fun getCurrentAccountBalance(): Double = _accountBalance.value
    fun getCurrentMarketPrice(symbol: String): Double? = _marketPrices.value[symbol]
    fun getCurrentOpenTradesCount(): Int = _openTrades.value.size
    fun isConnected(): Boolean = _connectionStatus.value
    
    // Manual sync triggers for UI
    suspend fun forceSyncBalance() {
        if (webSocketManager.isConnected()) {
            webSocketManager.subscribeBalance()
        }
    }
    
    suspend fun forceSyncPortfolio() {
        if (webSocketManager.isConnected()) {
            webSocketManager.subscribePortfolio()
        }
    }
    
    suspend fun subscribeToSymbol(symbol: String) {
        if (webSocketManager.isConnected()) {
            webSocketManager.subscribeTicks(listOf(symbol))
        }
    }
}

enum class SyncStatus {
    IDLE, SYNCING, ACTIVE, STOPPING, ERROR
}