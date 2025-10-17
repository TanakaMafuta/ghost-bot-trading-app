package com.ghostbot.trading.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostbot.trading.data.sync.DataSynchronizer
import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MarketAnalysisViewModel @Inject constructor(
    private val aiAnalysisUseCase: AIAnalysisUseCase,
    private val tradingUseCase: TradingUseCase,
    private val authenticationUseCase: AuthenticationUseCase,
    private val dataSynchronizer: DataSynchronizer
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MarketAnalysisUiState())
    val uiState: StateFlow<MarketAnalysisUiState> = _uiState.asStateFlow()
    
    init {
        loadMarketData()
    }
    
    fun selectSymbol(symbol: String) {
        _uiState.update { it.copy(selectedSymbol = symbol) }
        loadAnalysisForSymbol(symbol)
        subscribeToPrice(symbol)
    }
    
    fun selectTimeframe(timeframe: String) {
        _uiState.update { it.copy(selectedTimeframe = timeframe) }
        val currentSymbol = _uiState.value.selectedSymbol
        if (currentSymbol.isNotEmpty()) {
            generateTradingSignal(currentSymbol, timeframe)
        }
    }
    
    fun generateSignal() {
        val currentState = _uiState.value
        generateTradingSignal(currentState.selectedSymbol, currentState.selectedTimeframe)
    }
    
    fun executeTrade(signal: AISignal) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExecutingTrade = true) }
                
                val user = authenticationUseCase.getCurrentUser().first()
                val config = user?.tradingConfig ?: TradingConfig(tradingHours = TradingHours())
                
                val result = aiAnalysisUseCase.executeAIRecommendation(signal, config)
                
                if (result.isSuccess) {
                    val trade = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            isExecutingTrade = false,
                            lastExecutedTrade = trade,
                            successMessage = "Trade executed successfully"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isExecutingTrade = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Trade execution failed"
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to execute trade")
                _uiState.update {
                    it.copy(
                        isExecutingTrade = false,
                        errorMessage = e.message ?: "Trade execution failed"
                    )
                }
            }
        }
    }
    
    fun refreshAnalysis() {
        val currentSymbol = _uiState.value.selectedSymbol
        if (currentSymbol.isNotEmpty()) {
            loadAnalysisForSymbol(currentSymbol)
        }
    }
    
    fun dismissMessage() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null
            )
        }
    }
    
    private fun loadMarketData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Load available symbols
                tradingUseCase.getSymbols().take(1).collect { symbols ->
                    val popularSymbols = symbols.take(10) // Show top 10 popular symbols
                    
                    _uiState.update {
                        it.copy(
                            availableSymbols = popularSymbols,
                            selectedSymbol = popularSymbols.firstOrNull()?.name ?: "EURUSD",
                            isLoading = false
                        )
                    }
                    
                    // Load analysis for first symbol
                    val firstSymbol = popularSymbols.firstOrNull()?.name ?: "EURUSD"
                    loadAnalysisForSymbol(firstSymbol)
                    subscribeToPrice(firstSymbol)
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load market data")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to load market data"
                    )
                }
            }
        }
    }
    
    private fun loadAnalysisForSymbol(symbol: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingAnalysis = true) }
                
                // Load market analysis and AI signal in parallel
                combine(
                    aiAnalysisUseCase.getMarketAnalysis(symbol),
                    aiAnalysisUseCase.getAISignals(symbol).map { it.firstOrNull() },
                    flow { emit(aiAnalysisUseCase.getLatestSignal(symbol)) }
                ) { analysis, signals, latestSignal ->
                    AnalysisData(
                        analysis = analysis,
                        signals = signals,
                        latestSignal = latestSignal
                    )
                }.take(1).collect { data ->
                    _uiState.update {
                        it.copy(
                            isLoadingAnalysis = false,
                            currentAnalysis = data.analysis,
                            currentSignal = data.latestSignal,
                            signalHistory = data.signals?.let { signal -> listOf(signal) } ?: emptyList()
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to load analysis for $symbol")
                _uiState.update {
                    it.copy(
                        isLoadingAnalysis = false,
                        errorMessage = "Failed to load analysis: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun generateTradingSignal(symbol: String, timeframe: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isGeneratingSignal = true) }
                
                aiAnalysisUseCase.generateTradingSignal(symbol, timeframe).take(1).collect { signal ->
                    _uiState.update {
                        it.copy(
                            isGeneratingSignal = false,
                            currentSignal = signal,
                            signalHistory = listOf(signal) + it.signalHistory.take(9) // Keep last 10
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to generate signal")
                _uiState.update {
                    it.copy(
                        isGeneratingSignal = false,
                        errorMessage = "Failed to generate signal: ${e.message}"
                    )
                }
            }
        }
    }
    
    private fun subscribeToPrice(symbol: String) {
        viewModelScope.launch {
            try {
                dataSynchronizer.subscribeToSymbol(symbol)
                
                // Observe price updates
                dataSynchronizer.marketPrices.collect { prices ->
                    val currentPrice = prices[symbol]
                    if (currentPrice != null) {
                        _uiState.update {
                            it.copy(currentPrice = currentPrice)
                        }
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to subscribe to price for $symbol")
            }
        }
    }
    
    private data class AnalysisData(
        val analysis: MarketAnalysis,
        val signals: AISignal?,
        val latestSignal: AISignal?
    )
}

data class MarketAnalysisUiState(
    val isLoading: Boolean = false,
    val isLoadingAnalysis: Boolean = false,
    val isGeneratingSignal: Boolean = false,
    val isExecutingTrade: Boolean = false,
    val selectedSymbol: String = "",
    val selectedTimeframe: String = "M15",
    val availableSymbols: List<Symbol> = emptyList(),
    val availableTimeframes: List<String> = listOf("M1", "M5", "M15", "M30", "H1", "H4", "D1"),
    val currentPrice: Double = 0.0,
    val currentAnalysis: MarketAnalysis? = null,
    val currentSignal: AISignal? = null,
    val signalHistory: List<AISignal> = emptyList(),
    val lastExecutedTrade: Trade? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)