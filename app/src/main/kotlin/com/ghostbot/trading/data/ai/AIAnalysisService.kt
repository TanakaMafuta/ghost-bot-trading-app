package com.ghostbot.trading.data.ai

import com.ghostbot.trading.data.remote.api.OpenAIApiService
import com.ghostbot.trading.data.remote.api.PerplexityApiService
import com.ghostbot.trading.data.remote.dto.openai.*
import com.ghostbot.trading.data.remote.dto.perplexity.*
import com.ghostbot.trading.data.security.SecureStorage
import com.ghostbot.trading.domain.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIAnalysisService @Inject constructor(
    private val openAIApiService: OpenAIApiService,
    private val perplexityApiService: PerplexityApiService,
    private val secureStorage: SecureStorage
) {
    
    suspend fun analyzeMarket(
        symbol: String,
        marketData: Map<String, Any>,
        timeframe: String = "M15"
    ): Flow<MarketAnalysis> = flow {
        try {
            val apiKeys = secureStorage.getApiKeys()
            
            // Run multiple analyses in parallel
            coroutineScope {
                val technicalAnalysis = async {
                    getTechnicalAnalysis(symbol, marketData, timeframe, apiKeys?.openaiApiKey)
                }
                
                val sentimentAnalysis = async {
                    getSentimentAnalysis(symbol, apiKeys?.perplexityApiKey)
                }
                
                val newsAnalysis = async {
                    getNewsAnalysis(symbol, apiKeys?.perplexityApiKey)
                }
                
                val technical = technicalAnalysis.await()
                val sentiment = sentimentAnalysis.await()
                val news = newsAnalysis.await()
                
                // Combine all analyses
                val analysis = combineAnalyses(symbol, technical, sentiment, news)
                emit(analysis)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to analyze market for $symbol")
            emit(getDefaultAnalysis(symbol))
        }
    }
    
    suspend fun generateTradingSignal(
        symbol: String,
        marketData: Map<String, Any>,
        timeframe: String,
        riskLevel: RiskLevel
    ): Flow<AISignal> = flow {
        try {
            val apiKeys = secureStorage.getApiKeys()
            val openaiKey = apiKeys?.openaiApiKey
            
            if (openaiKey.isNullOrBlank()) {
                emit(getDefaultSignal(symbol, timeframe, "OpenAI API key not configured"))
                return@flow
            }
            
            val prompt = buildTradingPrompt(symbol, marketData, timeframe, riskLevel)
            
            val messages = listOf(
                ChatMessage("system", getTradingSystemPrompt(riskLevel)),
                ChatMessage("user", prompt)
            )
            
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = messages,
                temperature = 0.3,
                max_tokens = 800
            )
            
            val response = openAIApiService.getChatCompletion("Bearer $openaiKey", request)
            
            if (response.isSuccessful) {
                val completion = response.body()
                val content = completion?.choices?.firstOrNull()?.message?.content
                
                if (content != null) {
                    val signal = parseAIResponse(content, symbol, timeframe)
                    emit(signal)
                } else {
                    emit(getDefaultSignal(symbol, timeframe, "Empty AI response"))
                }
            } else {
                emit(getDefaultSignal(symbol, timeframe, "AI API request failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to generate trading signal")
            emit(getDefaultSignal(symbol, timeframe, "Error: ${e.message}"))
        }
    }
    
    suspend fun getMarketNews(
        symbol: String,
        limit: Int = 10
    ): Flow<List<NewsItem>> = flow {
        try {
            val apiKeys = secureStorage.getApiKeys()
            val perplexityKey = apiKeys?.perplexityApiKey
            
            if (perplexityKey.isNullOrBlank()) {
                emit(emptyList())
                return@flow
            }
            
            val query = "Latest news and market updates for $symbol trading pair financial markets"
            
            val request = PerplexityRequest(
                messages = listOf(
                    PerplexityMessage("user", query)
                ),
                max_tokens = 1000,
                temperature = 0.2,
                return_related_questions = false,
                search_recency_filter = "day"
            )
            
            val response = perplexityApiService.getChatCompletion("Bearer $perplexityKey", request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val newsItems = parseNewsFromContent(content, symbol)
                    emit(newsItems)
                } else {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get market news")
            emit(emptyList())
        }
    }
    
    suspend fun calculateRiskAssessment(
        symbol: String,
        tradeType: TradeType,
        volume: Double,
        marketData: Map<String, Any>
    ): Flow<RiskAssessment> = flow {
        try {
            val apiKeys = secureStorage.getApiKeys()
            val openaiKey = apiKeys?.openaiApiKey
            
            if (openaiKey.isNullOrBlank()) {
                emit(getDefaultRiskAssessment())
                return@flow
            }
            
            val prompt = buildRiskAssessmentPrompt(symbol, tradeType, volume, marketData)
            
            val messages = listOf(
                ChatMessage("system", "You are a risk management expert. Analyze the provided trade setup and provide risk assessment."),
                ChatMessage("user", prompt)
            )
            
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = messages,
                temperature = 0.1,
                max_tokens = 500
            )
            
            val response = openAIApiService.getChatCompletion("Bearer $openaiKey", request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (content != null) {
                    val assessment = parseRiskAssessment(content, symbol)
                    emit(assessment)
                } else {
                    emit(getDefaultRiskAssessment())
                }
            } else {
                emit(getDefaultRiskAssessment())
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to calculate risk assessment")
            emit(getDefaultRiskAssessment())
        }
    }
    
    private suspend fun getTechnicalAnalysis(
        symbol: String,
        marketData: Map<String, Any>,
        timeframe: String,
        apiKey: String?
    ): TechnicalAnalysisResult {
        if (apiKey.isNullOrBlank()) {
            return TechnicalAnalysisResult.default()
        }
        
        return try {
            val prompt = buildTechnicalAnalysisPrompt(symbol, marketData, timeframe)
            
            val messages = listOf(
                ChatMessage("system", "You are a technical analysis expert. Analyze price data and provide technical indicators assessment."),
                ChatMessage("user", prompt)
            )
            
            val request = ChatCompletionRequest(
                model = "gpt-4",
                messages = messages,
                temperature = 0.2,
                max_tokens = 400
            )
            
            val response = openAIApiService.getChatCompletion("Bearer $apiKey", request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                parseTechnicalAnalysis(content ?: "")
            } else {
                TechnicalAnalysisResult.default()
            }
        } catch (e: Exception) {
            Timber.e(e, "Technical analysis failed")
            TechnicalAnalysisResult.default()
        }
    }
    
    private suspend fun getSentimentAnalysis(
        symbol: String,
        apiKey: String?
    ): SentimentAnalysisResult {
        if (apiKey.isNullOrBlank()) {
            return SentimentAnalysisResult.neutral()
        }
        
        return try {
            val query = "Current market sentiment and trader mood for $symbol. What are traders saying?"
            
            val request = PerplexityRequest(
                messages = listOf(
                    PerplexityMessage("user", query)
                ),
                max_tokens = 300,
                temperature = 0.3,
                search_recency_filter = "day"
            )
            
            val response = perplexityApiService.getChatCompletion("Bearer $apiKey", request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                parseSentimentAnalysis(content ?: "")
            } else {
                SentimentAnalysisResult.neutral()
            }
        } catch (e: Exception) {
            Timber.e(e, "Sentiment analysis failed")
            SentimentAnalysisResult.neutral()
        }
    }
    
    private suspend fun getNewsAnalysis(
        symbol: String,
        apiKey: String?
    ): NewsAnalysisResult {
        if (apiKey.isNullOrBlank()) {
            return NewsAnalysisResult.empty()
        }
        
        return try {
            val query = "Recent financial news affecting $symbol price. Economic events and market moving news."
            
            val request = PerplexityRequest(
                messages = listOf(
                    PerplexityMessage("user", query)
                ),
                max_tokens = 400,
                temperature = 0.1,
                search_recency_filter = "day"
            )
            
            val response = perplexityApiService.getChatCompletion("Bearer $apiKey", request)
            
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                parseNewsAnalysis(content ?: "", symbol)
            } else {
                NewsAnalysisResult.empty()
            }
        } catch (e: Exception) {
            Timber.e(e, "News analysis failed")
            NewsAnalysisResult.empty()
        }
    }
    
    private fun buildTradingPrompt(
        symbol: String,
        marketData: Map<String, Any>,
        timeframe: String,
        riskLevel: RiskLevel
    ): String {
        return """
            Analyze $symbol on $timeframe timeframe for trading signal.
            
            Market Data:
            ${marketData.entries.joinToString("\n") { "${it.key}: ${it.value}" }}
            
            Risk Level: $riskLevel
            
            Provide:
            1. SIGNAL: BUY/SELL/HOLD
            2. CONFIDENCE: 0-100%
            3. ENTRY_PRICE: Recommended entry
            4. STOP_LOSS: Risk management level
            5. TAKE_PROFIT: Profit target
            6. REASONING: Brief explanation
            
            Format response as:
            SIGNAL: [BUY/SELL/HOLD]
            CONFIDENCE: [0-100]%
            ENTRY_PRICE: [price]
            STOP_LOSS: [price]
            TAKE_PROFIT: [price]
            REASONING: [explanation]
        """.trimIndent()
    }
    
    private fun getTradingSystemPrompt(riskLevel: RiskLevel): String {
        val riskDescription = when (riskLevel) {
            RiskLevel.CONSERVATIVE -> "very conservative, prioritize capital preservation, smaller position sizes"
            RiskLevel.MODERATE -> "balanced approach, moderate risk-reward ratio"
            RiskLevel.AGGRESSIVE -> "aggressive trading, higher risk for higher rewards"
        }
        
        return """
            You are an expert trading analyst with a $riskDescription trading style.
            Analyze market conditions and provide clear, actionable trading signals.
            Always include proper risk management with stop-loss and take-profit levels.
            Be precise and data-driven in your analysis.
        """.trimIndent()
    }
    
    private fun buildTechnicalAnalysisPrompt(
        symbol: String,
        marketData: Map<String, Any>,
        timeframe: String
    ): String {
        return """
            Technical analysis for $symbol on $timeframe:
            
            Market Data:
            ${marketData.entries.joinToString("\n") { "${it.key}: ${it.value}" }}
            
            Analyze:
            1. Trend direction (bullish/bearish/sideways)
            2. Key support/resistance levels
            3. Technical indicators (RSI, MACD, Moving Averages)
            4. Overall technical score (-100 to +100)
            
            Provide structured analysis.
        """.trimIndent()
    }
    
    private fun buildRiskAssessmentPrompt(
        symbol: String,
        tradeType: TradeType,
        volume: Double,
        marketData: Map<String, Any>
    ): String {
        return """
            Risk assessment for $tradeType $volume $symbol:
            
            Market Data:
            ${marketData.entries.joinToString("\n") { "${it.key}: ${it.value}" }}
            
            Assess:
            1. Risk level (LOW/MEDIUM/HIGH/EXTREME)
            2. Volatility impact
            3. Market conditions
            4. Position size appropriateness
            5. Risk score (0-100)
            
            Provide clear risk assessment.
        """.trimIndent()
    }
    
    private fun parseAIResponse(content: String, symbol: String, timeframe: String): AISignal {
        val lines = content.lines().map { it.trim() }
        
        val signal = lines.find { it.startsWith("SIGNAL:") }
            ?.substringAfter(":")
            ?.trim()
            ?.let { signalText ->
                when (signalText.uppercase()) {
                    "BUY" -> SignalType.BUY
                    "SELL" -> SignalType.SELL
                    else -> SignalType.HOLD
                }
            } ?: SignalType.HOLD
        
        val confidence = lines.find { it.startsWith("CONFIDENCE:") }
            ?.substringAfter(":")
            ?.replace("%", "")
            ?.trim()
            ?.toFloatOrNull()
            ?.div(100f) ?: 0.5f
        
        val entryPrice = lines.find { it.startsWith("ENTRY_PRICE:") }
            ?.substringAfter(":")
            ?.trim()
            ?.toDoubleOrNull()
        
        val stopLoss = lines.find { it.startsWith("STOP_LOSS:") }
            ?.substringAfter(":")
            ?.trim()
            ?.toDoubleOrNull()
        
        val takeProfit = lines.find { it.startsWith("TAKE_PROFIT:") }
            ?.substringAfter(":")
            ?.trim()
            ?.toDoubleOrNull()
        
        val reasoning = lines.find { it.startsWith("REASONING:") }
            ?.substringAfter(":")
            ?.trim() ?: content
        
        return AISignal(
            id = System.currentTimeMillis().toString(),
            symbol = symbol,
            signal = signal,
            confidence = confidence,
            recommendation = reasoning,
            entryPrice = entryPrice,
            stopLoss = stopLoss,
            takeProfit = takeProfit,
            timeframe = timeframe,
            timestamp = System.currentTimeMillis(),
            source = AISource.OPENAI,
            analysis = content
        )
    }
    
    // Helper functions for parsing responses and creating default values
    private fun parseTechnicalAnalysis(content: String): TechnicalAnalysisResult {
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
        
        return TechnicalAnalysisResult(trend, score, emptyList())
    }
    
    private fun parseSentimentAnalysis(content: String): SentimentAnalysisResult {
        val sentiment = when {
            content.contains("positive", ignoreCase = true) || content.contains("bullish", ignoreCase = true) -> MarketSentiment.BULLISH
            content.contains("negative", ignoreCase = true) || content.contains("bearish", ignoreCase = true) -> MarketSentiment.BEARISH
            else -> MarketSentiment.NEUTRAL
        }
        
        val score = when (sentiment) {
            MarketSentiment.BULLISH -> 0.6f
            MarketSentiment.BEARISH -> -0.6f
            else -> 0.0f
        }
        
        return SentimentAnalysisResult(sentiment, score)
    }
    
    private fun parseNewsAnalysis(content: String, symbol: String): NewsAnalysisResult {
        // Create simplified news items from content
        val newsItems = listOf(
            NewsItem(
                id = System.currentTimeMillis().toString(),
                title = "Market Analysis for $symbol",
                content = content.take(200),
                source = "AI Analysis",
                timestamp = System.currentTimeMillis(),
                impact = NewsImpact.MEDIUM,
                relatedSymbols = listOf(symbol),
                sentiment = 0.0f
            )
        )
        
        return NewsAnalysisResult(newsItems, 0.0f)
    }
    
    private fun parseNewsFromContent(content: String, symbol: String): List<NewsItem> {
        // Extract news items from AI response
        return listOf(
            NewsItem(
                id = System.currentTimeMillis().toString(),
                title = "Latest Market News for $symbol",
                content = content,
                source = "Perplexity AI",
                timestamp = System.currentTimeMillis(),
                impact = NewsImpact.MEDIUM,
                relatedSymbols = listOf(symbol),
                sentiment = 0.0f
            )
        )
    }
    
    private fun parseRiskAssessment(content: String, symbol: String): RiskAssessment {
        val riskLevel = when {
            content.contains("HIGH", ignoreCase = true) -> RiskLevel.AGGRESSIVE
            content.contains("LOW", ignoreCase = true) -> RiskLevel.CONSERVATIVE
            else -> RiskLevel.MODERATE
        }
        
        val riskScore = when (riskLevel) {
            RiskLevel.CONSERVATIVE -> 0.3f
            RiskLevel.MODERATE -> 0.6f
            RiskLevel.AGGRESSIVE -> 0.9f
        }
        
        return RiskAssessment(
            symbol = symbol,
            riskLevel = riskLevel,
            riskScore = riskScore,
            assessment = content,
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun combineAnalyses(
        symbol: String,
        technical: TechnicalAnalysisResult,
        sentiment: SentimentAnalysisResult,
        news: NewsAnalysisResult
    ): MarketAnalysis {
        val overallScore = (technical.score + sentiment.score + news.score) / 3f
        
        return MarketAnalysis(
            symbol = symbol,
            trend = technical.trend,
            sentiment = sentiment.sentiment,
            technicalScore = technical.score,
            fundamentalScore = 0.0f,
            newsScore = news.score,
            overallScore = overallScore,
            keyLevels = technical.keyLevels,
            newsItems = news.newsItems,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun getDefaultAnalysis(symbol: String): MarketAnalysis {
        return MarketAnalysis(
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
        )
    }
    
    private fun getDefaultSignal(symbol: String, timeframe: String, reason: String): AISignal {
        return AISignal(
            id = System.currentTimeMillis().toString(),
            symbol = symbol,
            signal = SignalType.HOLD,
            confidence = 0.0f,
            recommendation = "Signal unavailable: $reason",
            timeframe = timeframe,
            timestamp = System.currentTimeMillis(),
            source = AISource.INTERNAL,
            analysis = reason
        )
    }
    
    private fun getDefaultRiskAssessment(): RiskAssessment {
        return RiskAssessment(
            symbol = "",
            riskLevel = RiskLevel.MODERATE,
            riskScore = 0.5f,
            assessment = "Risk assessment unavailable",
            recommendations = emptyList(),
            timestamp = System.currentTimeMillis()
        )
    }
}

// Helper data classes
data class TechnicalAnalysisResult(
    val trend: TrendDirection,
    val score: Float,
    val keyLevels: List<PriceLevel>
) {
    companion object {
        fun default() = TechnicalAnalysisResult(
            trend = TrendDirection.UNKNOWN,
            score = 0.0f,
            keyLevels = emptyList()
        )
    }
}

data class SentimentAnalysisResult(
    val sentiment: MarketSentiment,
    val score: Float
) {
    companion object {
        fun neutral() = SentimentAnalysisResult(
            sentiment = MarketSentiment.NEUTRAL,
            score = 0.0f
        )
    }
}

data class NewsAnalysisResult(
    val newsItems: List<NewsItem>,
    val score: Float
) {
    companion object {
        fun empty() = NewsAnalysisResult(
            newsItems = emptyList(),
            score = 0.0f
        )
    }
}

data class RiskAssessment(
    val symbol: String,
    val riskLevel: RiskLevel,
    val riskScore: Float,
    val assessment: String,
    val recommendations: List<String>,
    val timestamp: Long
)