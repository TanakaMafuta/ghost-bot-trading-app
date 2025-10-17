package com.ghostbot.trading.data.remote.websocket

import com.ghostbot.trading.data.remote.dto.websocket.*
import com.ghostbot.trading.data.security.SecureStorage
import com.ghostbot.trading.domain.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DerivWebSocketManager @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
    private val secureStorage: SecureStorage
) : WebSocketListener() {
    
    private var webSocket: WebSocket? = null
    private val requestIdCounter = AtomicInteger(1)
    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<JsonElement>>()
    private val subscriptions = ConcurrentHashMap<String, String>() // subscriptionId -> symbol
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _marketData = MutableSharedFlow<MarketDataUpdate>()
    val marketData: Flow<MarketDataUpdate> = _marketData.asSharedFlow()
    
    private val _accountUpdates = MutableSharedFlow<AccountUpdate>()
    val accountUpdates: Flow<AccountUpdate> = _accountUpdates.asSharedFlow()
    
    private val _tradeUpdates = MutableSharedFlow<TradeUpdate>()
    val tradeUpdates: Flow<TradeUpdate> = _tradeUpdates.asSharedFlow()
    
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var authToken: String? = null
    private var isAuthorized = false
    
    suspend fun connect(appId: String = "1089"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                disconnect()
                
                _connectionState.value = ConnectionState.CONNECTING
                
                val wsUrl = "wss://ws.derivws.com/websockets/v3?app_id=$appId"
                val request = Request.Builder().url(wsUrl).build()
                
                webSocket = okHttpClient.newWebSocket(request, this@DerivWebSocketManager)
                
                // Wait for connection with timeout
                withTimeout(10_000) {
                    connectionState.first { it == ConnectionState.CONNECTED }
                }
                
                startPingJob()
                true
            } catch (e: Exception) {
                Timber.e(e, "Failed to connect to WebSocket")
                _connectionState.value = ConnectionState.ERROR
                false
            }
        }
    }
    
    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            reconnectJob?.cancel()
            pingJob?.cancel()
            
            webSocket?.close(1000, "Normal closure")
            webSocket = null
            
            authToken = null
            isAuthorized = false
            subscriptions.clear()
            pendingRequests.values.forEach { it.cancel() }
            pendingRequests.clear()
            
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    suspend fun authorize(token: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                authToken = token
                val request = mapOf(
                    "authorize" to token,
                    "req_id" to getNextRequestId()
                )
                
                val response = sendRequestWithResponse(request)
                val authResult = response.jsonObject["authorize"]
                
                if (authResult != null) {
                    isAuthorized = true
                    _connectionState.value = ConnectionState.AUTHORIZED
                    Timber.d("WebSocket authorized successfully")
                    true
                } else {
                    val error = response.jsonObject["error"]
                    Timber.e("Authorization failed: $error")
                    false
                }
            } catch (e: Exception) {
                Timber.e(e, "Authorization failed")
                false
            }
        }
    }
    
    suspend fun subscribeTicks(symbols: List<String>) {
        symbols.forEach { symbol ->
            try {
                val request = mapOf(
                    "ticks" to symbol,
                    "subscribe" to 1,
                    "req_id" to getNextRequestId()
                )
                
                val response = sendRequestWithResponse(request)
                val subscriptionId = response.jsonObject["subscription"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                
                if (subscriptionId != null) {
                    subscriptions[subscriptionId] = symbol
                    Timber.d("Subscribed to ticks for $symbol with ID $subscriptionId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to subscribe to ticks for $symbol")
            }
        }
    }
    
    suspend fun subscribePortfolio() {
        if (!isAuthorized) return
        
        try {
            val request = mapOf(
                "portfolio" to 1,
                "subscribe" to 1,
                "req_id" to getNextRequestId()
            )
            
            sendRequestWithResponse(request)
            Timber.d("Subscribed to portfolio updates")
        } catch (e: Exception) {
            Timber.e(e, "Failed to subscribe to portfolio")
        }
    }
    
    suspend fun subscribeBalance() {
        if (!isAuthorized) return
        
        try {
            val request = mapOf(
                "balance" to 1,
                "subscribe" to 1,
                "req_id" to getNextRequestId()
            )
            
            sendRequestWithResponse(request)
            Timber.d("Subscribed to balance updates")
        } catch (e: Exception) {
            Timber.e(e, "Failed to subscribe to balance")
        }
    }
    
    suspend fun buy(contractType: String, amount: Double, symbol: String, duration: Int): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isAuthorized) {
                    return@withContext Result.failure(Exception("Not authorized"))
                }
                
                // First get a proposal
                val proposalRequest = mapOf(
                    "proposal" to 1,
                    "contract_type" to contractType,
                    "currency" to "USD",
                    "amount" to amount,
                    "symbol" to symbol,
                    "duration" to duration,
                    "duration_unit" to "m",
                    "basis" to "stake",
                    "req_id" to getNextRequestId()
                )
                
                val proposalResponse = sendRequestWithResponse(proposalRequest)
                val proposalId = proposalResponse.jsonObject["proposal"]?.jsonObject?.get("id")?.jsonPrimitive?.content
                
                if (proposalId != null) {
                    // Execute buy
                    val buyRequest = mapOf(
                        "buy" to proposalId,
                        "price" to amount,
                        "req_id" to getNextRequestId()
                    )
                    
                    val buyResponse = sendRequestWithResponse(buyRequest)
                    val contractId = buyResponse.jsonObject["buy"]?.jsonObject?.get("contract_id")?.jsonPrimitive?.content
                    
                    if (contractId != null) {
                        Result.success(contractId)
                    } else {
                        val error = buyResponse.jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                        Result.failure(Exception(error ?: "Buy failed"))
                    }
                } else {
                    val error = proposalResponse.jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    Result.failure(Exception(error ?: "Proposal failed"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Buy operation failed")
                Result.failure(e)
            }
        }
    }
    
    suspend fun sell(contractId: String, price: Double): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (!isAuthorized) {
                    return@withContext Result.failure(Exception("Not authorized"))
                }
                
                val request = mapOf(
                    "sell" to contractId,
                    "price" to price,
                    "req_id" to getNextRequestId()
                )
                
                val response = sendRequestWithResponse(request)
                val transactionId = response.jsonObject["sell"]?.jsonObject?.get("transaction_id")?.jsonPrimitive?.content
                
                if (transactionId != null) {
                    Result.success(transactionId)
                } else {
                    val error = response.jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    Result.failure(Exception(error ?: "Sell failed"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Sell operation failed")
                Result.failure(e)
            }
        }
    }
    
    fun isConnected(): Boolean {
        return connectionState.value in listOf(ConnectionState.CONNECTED, ConnectionState.AUTHORIZED)
    }
    
    fun isAuthorizedState(): Boolean {
        return connectionState.value == ConnectionState.AUTHORIZED
    }
    
    // WebSocketListener implementation
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Timber.d("WebSocket connection opened")
        _connectionState.value = ConnectionState.CONNECTED
        
        // Auto-authorize if token is available
        authToken?.let { token ->
            reconnectScope.launch {
                authorize(token)
            }
        }
    }
    
    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val jsonElement = json.parseToJsonElement(text)
            handleMessage(jsonElement)
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse WebSocket message: $text")
        }
    }
    
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Timber.d("WebSocket closing: $code - $reason")
    }
    
    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Timber.d("WebSocket closed: $code - $reason")
        _connectionState.value = ConnectionState.DISCONNECTED
        
        // Start reconnection if not manually closed
        if (code != 1000) {
            startReconnection()
        }
    }
    
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Timber.e(t, "WebSocket failure")
        _connectionState.value = ConnectionState.ERROR
        startReconnection()
    }
    
    private fun handleMessage(jsonElement: JsonElement) {
        val jsonObject = jsonElement.jsonObject
        
        // Handle request responses
        val reqId = jsonObject["req_id"]?.jsonPrimitive?.content?.toIntOrNull()
        if (reqId != null) {
            pendingRequests[reqId]?.complete(jsonElement)
            pendingRequests.remove(reqId)
        }
        
        // Handle subscription updates
        when {
            jsonObject.containsKey("tick") -> handleTickUpdate(jsonObject)
            jsonObject.containsKey("portfolio") -> handlePortfolioUpdate(jsonObject)
            jsonObject.containsKey("balance") -> handleBalanceUpdate(jsonObject)
            jsonObject.containsKey("proposal_open_contract") -> handleContractUpdate(jsonObject)
        }
    }
    
    private fun handleTickUpdate(jsonObject: Map<String, JsonElement>) {
        try {
            val tick = jsonObject["tick"]?.jsonObject
            if (tick != null) {
                val symbol = tick["symbol"]?.jsonPrimitive?.content ?: return
                val quote = tick["quote"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return
                val epoch = tick["epoch"]?.jsonPrimitive?.content?.toLongOrNull() ?: return
                
                val marketUpdate = MarketDataUpdate.PriceUpdate(
                    symbol = symbol,
                    price = quote,
                    timestamp = epoch * 1000
                )
                
                _marketData.tryEmit(marketUpdate)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle tick update")
        }
    }
    
    private fun handlePortfolioUpdate(jsonObject: Map<String, JsonElement>) {
        try {
            val portfolio = jsonObject["portfolio"]?.jsonObject
            if (portfolio != null) {
                val update = AccountUpdate.PortfolioUpdate(
                    contracts = emptyList(), // Parse contracts from response
                    timestamp = System.currentTimeMillis()
                )
                
                _accountUpdates.tryEmit(update)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle portfolio update")
        }
    }
    
    private fun handleBalanceUpdate(jsonObject: Map<String, JsonElement>) {
        try {
            val balance = jsonObject["balance"]?.jsonObject
            if (balance != null) {
                val balanceValue = balance["balance"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return
                val currency = balance["currency"]?.jsonPrimitive?.content ?: "USD"
                
                val update = AccountUpdate.BalanceUpdate(
                    balance = balanceValue,
                    currency = currency,
                    timestamp = System.currentTimeMillis()
                )
                
                _accountUpdates.tryEmit(update)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle balance update")
        }
    }
    
    private fun handleContractUpdate(jsonObject: Map<String, JsonElement>) {
        try {
            val contract = jsonObject["proposal_open_contract"]?.jsonObject
            if (contract != null) {
                val contractId = contract["contract_id"]?.jsonPrimitive?.content ?: return
                val profit = contract["profit"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
                
                val update = TradeUpdate(
                    contractId = contractId,
                    profit = profit,
                    timestamp = System.currentTimeMillis()
                )
                
                _tradeUpdates.tryEmit(update)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle contract update")
        }
    }
    
    private suspend fun sendRequestWithResponse(request: Map<String, Any>): JsonElement {
        return withTimeout(30_000) {
            val requestId = request["req_id"] as Int
            val deferred = CompletableDeferred<JsonElement>()
            
            pendingRequests[requestId] = deferred
            
            val jsonString = json.encodeToString(request)
            webSocket?.send(jsonString) ?: throw Exception("WebSocket not connected")
            
            try {
                deferred.await()
            } finally {
                pendingRequests.remove(requestId)
            }
        }
    }
    
    private fun startPingJob() {
        pingJob?.cancel()
        pingJob = reconnectScope.launch {
            while (isActive && isConnected()) {
                delay(30_000) // Ping every 30 seconds
                try {
                    val pingRequest = mapOf(
                        "ping" to 1,
                        "req_id" to getNextRequestId()
                    )
                    webSocket?.send(json.encodeToString(pingRequest))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to send ping")
                    break
                }
            }
        }
    }
    
    private fun startReconnection() {
        if (reconnectJob?.isActive == true) return
        
        reconnectJob = reconnectScope.launch {
            var attempt = 0
            val maxAttempts = 5
            
            while (attempt < maxAttempts && !isConnected()) {
                attempt++
                val delay = minOf(1000L * (1L shl attempt), 30_000L) // Exponential backoff, max 30s
                
                Timber.d("Reconnection attempt $attempt/$maxAttempts in ${delay}ms")
                delay(delay)
                
                try {
                    if (connect()) {
                        Timber.d("Reconnected successfully")
                        
                        // Re-authorize if token is available
                        authToken?.let { token ->
                            if (authorize(token)) {
                                // Re-subscribe to previous subscriptions
                                resubscribeAll()
                            }
                        }
                        break
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Reconnection attempt $attempt failed")
                }
            }
            
            if (attempt >= maxAttempts) {
                Timber.e("All reconnection attempts failed")
                _connectionState.value = ConnectionState.ERROR
            }
        }
    }
    
    private suspend fun resubscribeAll() {
        try {
            // Re-subscribe to portfolio and balance
            subscribePortfolio()
            subscribeBalance()
            
            // Re-subscribe to ticks for all symbols
            val symbols = subscriptions.values.toList()
            if (symbols.isNotEmpty()) {
                subscribeTicks(symbols)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to resubscribe")
        }
    }
    
    private fun getNextRequestId(): Int {
        return requestIdCounter.getAndIncrement()
    }
}

// Connection states
enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, AUTHORIZED, ERROR
}

// Update types
sealed class MarketDataUpdate {
    data class PriceUpdate(
        val symbol: String,
        val price: Double,
        val timestamp: Long
    ) : MarketDataUpdate()
    
    data class CandleUpdate(
        val symbol: String,
        val open: Double,
        val high: Double,
        val low: Double,
        val close: Double,
        val timestamp: Long
    ) : MarketDataUpdate()
}

sealed class AccountUpdate {
    data class BalanceUpdate(
        val balance: Double,
        val currency: String,
        val timestamp: Long
    ) : AccountUpdate()
    
    data class PortfolioUpdate(
        val contracts: List<Any>, // Define proper contract type
        val timestamp: Long
    ) : AccountUpdate()
}

data class TradeUpdate(
    val contractId: String,
    val profit: Double,
    val timestamp: Long
)