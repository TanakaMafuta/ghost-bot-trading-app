package com.ghostbot.trading.data.repository

import com.ghostbot.trading.data.local.dao.AISignalDao
import com.ghostbot.trading.data.local.entity.AISignalEntity
import com.ghostbot.trading.data.remote.api.OpenAIApiService
import com.ghostbot.trading.data.remote.api.PerplexityApiService
import com.ghostbot.trading.data.remote.dto.openai.*
import com.ghostbot.trading.data.remote.dto.perplexity.*
import com.ghostbot.trading.data.security.SecureStorage
import com.ghostbot.trading.domain.model.*
import com.ghostbot.trading.domain.repository.AIRepository
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val openAIApiService: OpenAIApiService,
    private val perplexityApiService: PerplexityApiService,
    private val aiSignalDao: AISignalDao,
    private val secureStorage: SecureStorage,
    private val json: Json
) : AIRepository {
    
    override suspend fun getMarketAnalysis(symbol: String): Flow<MarketAnalysis> = flow {
        try {
            val apiKeys = secureStorage.getApiKeys()
            
            // Get technical analysis from OpenAI
            val technicalAnalysis = getTechnicalAnalysis(symbol, apiKeys?.openaiApiKey)
            
            // Get news sentiment from Perplexity
            val newsSentiment = getNewsSentiment(symbol, apiKeys?.perplexityApiKey)
            
            // Combine analyses
            val analysis = MarketAnalysis(
                symbol = symbol,
                trend = technicalAnalysis.trend,
                sentiment = newsSentiment.sentiment,
                technicalScore = technicalAnalysis.score,
                fundamentalScore = 0.0f, // Could be enhanced
                newsScore = newsSentiment.score,
                overallScore = (technicalAnalysis.score + newsSentiment.score) / 2,
                keyLevels = technicalAnalysis.keyLevels,
                newsItems = newsSentiment.newsItems,
                timestamp = System.currentTimeMillis()
            )
            
            emit(analysis)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get market analysis")
            // Emit default analysis
            emit(MarketAnalysis(
                symbol = symbol,
                trend = TrendDirection.UNKNOWN,
                sentiment = MarketSentiment.NEUTRAL,
                technicalScore = 0.0f,
                fundamentalScore = 0.0f,
                newsScore = 0.0f,
                overallScore = 0.0f,
                keyLevels = emptyList(),
                newsItems = emptyList(),
                timestamp = System.currentTimeMillis()
            ))
        }
    }
    
    override suspend fun generateTradingSignal(
        symbol: String,
        timeframe: String,
        marketData: Map<String, Any>
    ): Flow<AISignal> = flow {
        try {
            val apiKeys = secureStorage.getApiKeys()
            val openaiKey = apiKeys?.openaiApiKey
            
            if (openaiKey != null) {
                val prompt = buildTradingPrompt(symbol, timeframe, marketData)
                val messages = listOf(
                    ChatMessage("system", "You are an expert trading analyst. Analyze the provided market data and generate a clear trading signal."),
                    ChatMessage("user", prompt)
                )
                
                val request = ChatCompletionRequest(
                    model = "gpt-4",
                    messages = messages,
                    temperature = 0.3,
                    max_tokens = 500
                )
                
                val response = openAIApiService.getChatCompletion("Bearer $openaiKey", request)
                
                if (response.isSuccessful) {
                    val completion = response.body()
                    val content = completion?.choices?.firstOrNull()?.message?.content
                    
                    if (content != null) {
                        val signal = parseAISignalFromContent(content, symbol, timeframe)
                        
                        // Save signal to local database
                        val signalEntity = AISignalEntity(
                            id = signal.id,
                            symbol = signal.symbol,
                            signal = signal.signal,
                            confidence = signal.confidence,
                            recommendation = signal.recommendation,
                            entryPrice = signal.entryPrice,
                            stopLoss = signal.stopLoss,
                            takeProfit = signal.takeProfit,
                            timeframe = signal.timeframe,
                            timestamp = signal.timestamp,
                            source = signal.source,
                            analysis = signal.analysis,
                            metadata = json.encodeToString(signal.metadata)
                        )
                        aiSignalDao.insertSignal(signalEntity)
                        
                        emit(signal)
                    } else {
                        throw Exception("Empty response from AI")
                    }
                } else {
                    throw Exception("AI API request failed: ${response.message()}")
                }
            } else {
                throw Exception("OpenAI API key not configured")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate trading signal")
            
            // Emit default signal
            val defaultSignal = AISignal(
                id = System.currentTimeMillis().toString(),
                symbol = symbol,
                signal = SignalType.HOLD,
                confidence = 0.0f,
                recommendation = "Unable to generate signal at this time",
                timeframe = timeframe,
                timestamp = System.currentTimeMillis(),
                source = AISource.OPENAI,
                analysis = "Error: ${e.message}"
            )
            
            emit(defaultSignal)
        }
    }
    
    override suspend fun getAISignals(symbol: String): Flow<List<AISignal>> {
        return aiSignalDao.getSignalsBySymbol(symbol).map { entities ->
            entities.map { entity ->
                AISignal(
                    id = entity.id,
                    symbol = entity.symbol,
                    signal = entity.signal,
                    confidence = entity.confidence,
                    recommendation = entity.recommendation,
                    entryPrice = entity.entryPrice,
                    stopLoss = entity.stopLoss,
                    takeProfit = entity.takeProfit,
                    timeframe = entity.timeframe,
                    timestamp = entity.timestamp,
                    source = entity.source,
                    analysis = entity.analysis,
                    metadata = try {
                        json.decodeFromString(entity.metadata)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                )
            }
        }
    }
    
    override suspend fun getLatestSignal(symbol: String): AISignal? {
        return try {
            val entity = aiSignalDao.getLatestSignalForSymbol(symbol)
            entity?.let {
                AISignal(
                    id = it.id,
                    symbol = it.symbol,
                    signal = it.signal,
                    confidence = it.confidence,
                    recommendation = it.recommendation,
                    entryPrice = it.entryPrice,
                    stopLoss = it.stopLoss,
                    takeProfit = it.takeProfit,
                    timeframe = it.timeframe,
                    timestamp = it.timestamp,
                    source = it.source,
                    analysis = it.analysis,
                    metadata = try {
                        json.decodeFromString(it.metadata)
                    } catch (e: Exception) {
                        emptyMap()
                    }
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get latest signal")
            null
        }
    }
    
    private data class TechnicalAnalysisResult(
        val trend: TrendDirection,
        val score: Float,
        val keyLevels: List<PriceLevel>
    )
    
    private data class NewsSentimentResult(
        val sentiment: MarketSentiment,
        val score: Float,
        val newsItems: List<NewsItem>
    )
    
    private suspend fun getTechnicalAnalysis(symbol: String, apiKey: String?): TechnicalAnalysisResult {
        return try {
            if (apiKey != null) {
                val prompt = "Provide technical analysis for $symbol including trend direction and key support/resistance levels."
                val messages = listOf(
                    ChatMessage("system", "You are a technical analysis expert. Provide concise, actionable analysis."),
                    ChatMessage("user", prompt)
                )
                
                val request = ChatCompletionRequest(
                    model = "gpt-4",
                    messages = messages,
                    temperature = 0.2,
                    max_tokens = 300
                )
                
                val response = openAIApiService.getChatCompletion("Bearer $apiKey", request)
                
                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    parseTechnicalAnalysis(content ?: "")
                } else {
                    getDefaultTechnicalAnalysis()
                }
            } else {
                getDefaultTechnicalAnalysis()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get technical analysis")
            getDefaultTechnicalAnalysis()
        }
    }
    
    private suspend fun getNewsSentiment(symbol: String, apiKey: String?): NewsSentimentResult {
        return try {
            if (apiKey != null) {
                val request = PerplexityRequest(
                    messages = listOf(
                        PerplexityMessage("user", "What is the current market sentiment for $symbol based on recent news?")
                    ),
                    max_tokens = 500,
                    temperature = 0.3
                )
                
                val response = perplexityApiService.getChatCompletion("Bearer $apiKey", request)
                
                if (response.isSuccessful) {
                    val content = response.body()?.choices?.firstOrNull()?.message?.content
                    parseNewsSentiment(content ?: "")
                } else {
                    getDefaultNewsSentiment()
                }
            } else {
                getDefaultNewsSentiment()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get news sentiment")
            getDefaultNewsSentiment()
        }
    }
    
    private fun buildTradingPrompt(symbol: String, timeframe: String, marketData: Map<String, Any>): String {
        return """
            Analyze $symbol on $timeframe timeframe.
            Market Data: $marketData
            
            Provide:
            1. Trading Signal: BUY/SELL/HOLD
            2. Confidence Level: 0-100%
            3. Entry Price (if applicable)
            4. Stop Loss Level
            5. Take Profit Target
            6. Reasoning
            
            Format your response as structured data.
        """.trimIndent()
    }
    
    private fun parseAISignalFromContent(content: String, symbol: String, timeframe: String): AISignal {
        // Parse AI response content into structured signal
        // This is a simplified parser - in production, you'd want more robust parsing
        val signalType = when {
            content.contains("BUY", ignoreCase = true) -> SignalType.BUY
            content.contains("SELL", ignoreCase = true) -> SignalType.SELL
            else -> SignalType.HOLD
        }
        
        val confidence = extractConfidence(content)
        
        return AISignal(
            id = System.currentTimeMillis().toString(),
            symbol = symbol,
            signal = signalType,
            confidence = confidence,
            recommendation = content,
            timeframe = timeframe,
            timestamp = System.currentTimeMillis(),
            source = AISource.OPENAI,
            analysis = content
        )
    }
    
    private fun extractConfidence(content: String): Float {
        // Extract confidence percentage from content
        val regex = "(\\d+)%".toRegex()
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.toFloatOrNull()?.div(100f) ?: 0.5f
    }
    
    private fun parseTechnicalAnalysis(content: String): TechnicalAnalysisResult {
        // Simple parsing - in production, use more sophisticated NLP
        val trend = when {
            content.contains("bullish", ignoreCase = true) -> TrendDirection.BULLISH
            content.contains("bearish", ignoreCase = true) -> TrendDirection.BEARISH
            else -> TrendDirection.SIDEWAYS
        }
        
        val score = when (trend) {
            TrendDirection.BULLISH -> 0.7f
            TrendDirection.BEARISH -> -0.7f
            else -> 0.0f
        }
        
        return TechnicalAnalysisResult(
            trend = trend,
            score = score,
            keyLevels = emptyList() // Could parse support/resistance levels
        )
    }
    
    private fun parseNewsSentiment(content: String): NewsSentimentResult {
        val sentiment = when {
            content.contains("positive", ignoreCase = true) -> MarketSentiment.BULLISH
            content.contains("negative", ignoreCase = true) -> MarketSentiment.BEARISH
            else -> MarketSentiment.NEUTRAL
        }
        
        val score = when (sentiment) {
            MarketSentiment.BULLISH -> 0.6f
            MarketSentiment.BEARISH -> -0.6f
            else -> 0.0f
        }
        
        return NewsSentimentResult(
            sentiment = sentiment,
            score = score,
            newsItems = emptyList() // Could parse individual news items
        )
    }
    
    private fun getDefaultTechnicalAnalysis(): TechnicalAnalysisResult {
        return TechnicalAnalysisResult(
            trend = TrendDirection.UNKNOWN,
            score = 0.0f,
            keyLevels = emptyList()
        )
    }
    
    private fun getDefaultNewsSentiment(): NewsSentimentResult {
        return NewsSentimentResult(
            sentiment = MarketSentiment.NEUTRAL,
            score = 0.0f,
            newsItems = emptyList()
        )
    }
}