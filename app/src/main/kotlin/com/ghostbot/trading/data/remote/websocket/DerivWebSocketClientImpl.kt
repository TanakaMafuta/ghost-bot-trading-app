package com.ghostbot.trading.data.remote.websocket

import com.ghostbot.trading.data.remote.dto.websocket.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DerivWebSocketClientImpl @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json
) : DerivWebSocketClient, WebSocketListener() {
    
    private var webSocket: WebSocket? = null
    private val requestIdCounter = AtomicInteger(1)
    private val pendingRequests = mutableMapOf<Int, CompletableDeferred<String>>()
    
    private val _connectionState = MutableStateFlow(WebSocketState.DISCONNECTED)
    override val connectionState: StateFlow<WebSocketState> = _connectionState.asStateFlow()
    
    private val _messages = MutableSharedFlow<WebSocketMessage>()
    override val messages: Flow<WebSocketMessage> = _messages.asSharedFlow()
    
    private var authToken: String? = null
    private var isAuthorizedFlag = false
    
    override suspend fun connect(url: String, appId: String) {
        if (webSocket != null) {
            disconnect()
        }
        
        _connectionState.value = WebSocketState.CONNECTING
        
        val request = Request.Builder()
            .url("$url?app_id=$appId")
            .build()
        
        webSocket = okHttpClient.newWebSocket(request, this)
    }
    
    override suspend fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        authToken = null
        isAuthorizedFlag = false
        _connectionState.value = WebSocketState.DISCONNECTED
    }
    
    override suspend fun authorize(token: String): Boolean {
        authToken = token
        val request = AuthorizeRequest(
            authorize = token,
            req_id = getNextRequestId()
        )
        
        return try {
            val response = sendRequestAndWaitForResponse(request)
            val authResponse = json.decodeFromString<AuthorizeResponse>(response)
            isAuthorizedFlag = authResponse.authorize != null
            if (isAuthorizedFlag) {
                _connectionState.value = WebSocketState.AUTHORIZED
            }
            isAuthorizedFlag
        } catch (e: Exception) {
            Timber.e(e, "Authorization failed")
            false
        }
    }
    
    override suspend fun subscribeTicks(symbols: List<String>) {
        symbols.forEach { symbol ->
            val request = TicksRequest(
                ticks = symbol,
                subscribe = 1,
                req_id = getNextRequestId()
            )
            sendRequest(request)
        }
    }
    
    override suspend fun subscribeCandles(symbol: String, granularity: Int) {
        val request = CandlesRequest(
            ticks_history = symbol,
            granularity = granularity,
            subscribe = 1,
            req_id = getNextRequestId()
        )
        sendRequest(request)
    }
    
    override suspend fun subscribePortfolio() {
        if (!isAuthorized()) return
        
        val request = PortfolioRequest(
            portfolio = 1,
            subscribe = 1,
            req_id = getNextRequestId()
        )
        sendRequest(request)
    }
    
    override suspend fun subscribeBalance() {
        if (!isAuthorized()) return
        
        val request = BalanceRequest(
            balance = 1,
            subscribe = 1,
            req_id = getNextRequestId()
        )
        sendRequest(request)
    }
    
    override suspend fun subscribeProposalOpenContract() {
        if (!isAuthorized()) return
        
        val request = ProposalOpenContractRequest(
            proposal_open_contract = 1,
            subscribe = 1,
            req_id = getNextRequestId()
        )
        sendRequest(request)
    }
    
    override suspend fun unsubscribeTicks(symbols: List<String>) {
        // Implementation for unsubscribing from ticks
        symbols.forEach { symbol ->
            val request = mapOf(
                "forget" to symbol,
                "req_id" to getNextRequestId()
            )
            sendRequest(request)
        }
    }
    
    override suspend fun unsubscribeCandles(symbol: String) {
        // Implementation for unsubscribing from candles
    }
    
    override suspend fun unsubscribePortfolio() {
        // Implementation for unsubscribing from portfolio
    }
    
    override suspend fun unsubscribeBalance() {
        // Implementation for unsubscribing from balance
    }
    
    override suspend fun buy(proposal: BuyProposal): TradeResult {
        if (!isAuthorized()) throw IllegalStateException("Not authorized")
        
        val request = BuyRequest(
            buy = proposal.id,
            price = proposal.price,
            req_id = getNextRequestId()
        )
        
        return try {
            val response = sendRequestAndWaitForResponse(request)
            val buyResponse = json.decodeFromString<BuyResponse>(response)
            TradeResult.Success(buyResponse.buy)
        } catch (e: Exception) {
            Timber.e(e, "Buy request failed")
            TradeResult.Error(e.message ?: "Unknown error")
        }
    }
    
    override suspend fun sell(contractId: String, price: Double): TradeResult {
        if (!isAuthorized()) throw IllegalStateException("Not authorized")
        
        val request = SellRequest(
            sell = contractId,
            price = price,
            req_id = getNextRequestId()
        )
        
        return try {
            val response = sendRequestAndWaitForResponse(request)
            val sellResponse = json.decodeFromString<SellResponse>(response)
            TradeResult.Success(sellResponse.sell)
        } catch (e: Exception) {
            Timber.e(e, "Sell request failed")
            TradeResult.Error(e.message ?: "Unknown error")
        }
    }
    
    override suspend fun getAccountStatus(): AccountStatus {
        TODO("Implement account status")
    }
    
    override suspend fun getPortfolio(): Portfolio {
        TODO("Implement get portfolio")
    }
    
    override suspend fun getBalance(): Balance {
        TODO("Implement get balance")
    }
    
    override suspend fun getActiveSymbols(): List<Symbol> {
        TODO("Implement get active symbols")
    }
    
    override suspend fun getTicks(symbol: String, count: Int): TicksHistory {
        TODO("Implement get ticks")
    }
    
    override fun isConnected(): Boolean {
        return connectionState.value == WebSocketState.CONNECTED || connectionState.value == WebSocketState.AUTHORIZED
    }
    
    override fun isAuthorized(): Boolean {
        return isAuthorizedFlag && connectionState.value == WebSocketState.AUTHORIZED
    }
    
    // WebSocketListener implementation
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Timber.d("WebSocket opened")
        _connectionState.value = WebSocketState.CONNECTED
        _messages.tryEmit(WebSocketMessage.Connected("Connected to Deriv API"))
    }
    
    override fun onMessage(webSocket: WebSocket, text: String) {
        Timber.d("WebSocket message received: $text")
        handleMessage(text)
    }
    
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Timber.d("WebSocket closing: $code - $reason")
        _connectionState.value = WebSocketState.DISCONNECTED
        _messages.tryEmit(WebSocketMessage.Disconnected(reason))
    }
    
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Timber.e(t, "WebSocket failure")
        _connectionState.value = WebSocketState.ERROR
        _messages.tryEmit(WebSocketMessage.Error(t.message ?: "WebSocket failure"))
    }
    
    private fun handleMessage(message: String) {
        try {
            // Parse message and emit appropriate events
            val jsonElement = json.parseToJsonElement(message)
            // Implementation depends on Deriv API message format
            // This would parse different message types and emit corresponding events
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse WebSocket message")
            _messages.tryEmit(WebSocketMessage.Error("Failed to parse message: ${e.message}"))
        }
    }
    
    private fun getNextRequestId(): Int {
        return requestIdCounter.getAndIncrement()
    }
    
    private suspend fun sendRequest(request: Any) {
        val jsonString = json.encodeToString(request)
        webSocket?.send(jsonString)
    }
    
    private suspend fun sendRequestAndWaitForResponse(request: Any): String {
        return withTimeout(30_000) { // 30 second timeout
            val deferred = CompletableDeferred<String>()
            val requestId = when (request) {
                is AuthorizeRequest -> request.req_id
                is BuyRequest -> request.req_id
                is SellRequest -> request.req_id
                else -> getNextRequestId()
            }
            
            pendingRequests[requestId] = deferred
            sendRequest(request)
            
            try {
                deferred.await()
            } finally {
                pendingRequests.remove(requestId)
            }
        }
    }
}

sealed class TradeResult {
    data class Success(val contract: Any) : TradeResult() // Replace Any with proper contract type
    data class Error(val message: String) : TradeResult()
}