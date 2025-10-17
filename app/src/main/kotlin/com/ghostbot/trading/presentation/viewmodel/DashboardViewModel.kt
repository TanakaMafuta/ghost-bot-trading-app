package com.ghostbot.trading.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostbot.trading.data.sync.DataSynchronizer
import com.ghostbot.trading.data.sync.SyncStatus
import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tradingUseCase: TradingUseCase,
    private val aiAnalysisUseCase: AIAnalysisUseCase,
    private val authenticationUseCase: AuthenticationUseCase,
    private val dataSynchronizer: DataSynchronizer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboardData()
        observeDataChanges()
    }
    
    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Get current user
                val user = authenticationUseCase.getCurrentUser().first()
                val accountId = user?.activeAccountId ?: "default"
                
                // Collect dashboard data streams
                combine(
                    dataSynchronizer.accountBalance,
                    dataSynchronizer.openTrades,
                    dataSynchronizer.connectionStatus,
                    dataSynchronizer.syncStatus
                ) { balance, trades, isConnected, syncStatus ->
                    DashboardData(
                        balance = balance,
                        openTrades = trades,
                        isConnected = isConnected,
                        syncStatus = syncStatus
                    )
                }.collect { data ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            balance = data.balance,
                            openTrades = data.openTrades,
                            isConnected = data.isConnected,
                            syncStatus = data.syncStatus,
                            errorMessage = null
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load dashboard data")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Unknown error occurred"
                    )
                }
            }
        }
    }
    
    fun refreshData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isRefreshing = true) }
                
                // Force sync all data
                dataSynchronizer.forceSyncBalance()
                dataSynchronizer.forceSyncPortfolio()
                
                val user = authenticationUseCase.getCurrentUser().first()
                val accountId = user?.activeAccountId ?: "default"
                dataSynchronizer.syncTradeHistory(accountId, forceRefresh = true)
                
                _uiState.update { it.copy(isRefreshing = false) }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to refresh data")
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = e.message ?: "Failed to refresh data"
                    )
                }
            }
        }
    }
    
    fun loadAIInsights() {
        viewModelScope.launch {
            try {
                // Get popular symbols
                val symbols = listOf("EURUSD", "GBPUSD", "USDJPY")
                val insights = mutableListOf<AISignal>()
                
                symbols.forEach { symbol ->
                    aiAnalysisUseCase.getLatestSignal(symbol)?.let { signal ->
                        insights.add(signal)
                    }
                }
                
                _uiState.update {
                    it.copy(aiInsights = insights)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load AI insights")
            }
        }
    }
    
    fun generateQuickSignal(symbol: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isGeneratingSignal = true) }
                
                aiAnalysisUseCase.generateTradingSignal(symbol).take(1).collect { signal ->
                    _uiState.update { currentState ->
                        val updatedInsights = currentState.aiInsights.toMutableList()
                        val existingIndex = updatedInsights.indexOfFirst { it.symbol == symbol }
                        
                        if (existingIndex >= 0) {
                            updatedInsights[existingIndex] = signal
                        } else {
                            updatedInsights.add(signal)
                        }
                        
                        currentState.copy(
                            aiInsights = updatedInsights,
                            isGeneratingSignal = false
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate signal for $symbol")
                _uiState.update {
                    it.copy(
                        isGeneratingSignal = false,
                        errorMessage = "Failed to generate signal: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun observeDataChanges() {
        viewModelScope.launch {
            // Start data synchronization
            dataSynchronizer.startSynchronization()
            
            // Load AI insights periodically
            loadAIInsights()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        dataSynchronizer.stopSynchronization()
    }
    
    private data class DashboardData(
        val balance: Double,
        val openTrades: List<Trade>,
        val isConnected: Boolean,
        val syncStatus: SyncStatus
    )
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isGeneratingSignal: Boolean = false,
    val balance: Double = 0.0,
    val openTrades: List<Trade> = emptyList(),
    val aiInsights: List<AISignal> = emptyList(),
    val isConnected: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val errorMessage: String? = null
)