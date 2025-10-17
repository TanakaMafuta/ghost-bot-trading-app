package com.ghostbot.trading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AISignal(
    val id: String,
    val symbol: String,
    val signal: SignalType,
    val confidence: Float, // 0.0 to 1.0
    val recommendation: String,
    val entryPrice: Double? = null,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val timeframe: String,
    val timestamp: Long,
    val source: AISource,
    val analysis: String,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class SignalType {
    BUY, SELL, HOLD, STRONG_BUY, STRONG_SELL
}

@Serializable
enum class AISource {
    OPENAI, PERPLEXITY, INTERNAL, COMBINED
}

@Serializable
data class MarketAnalysis(
    val symbol: String,
    val trend: TrendDirection,
    val sentiment: MarketSentiment,
    val technicalScore: Float, // -1.0 (bearish) to 1.0 (bullish)
    val fundamentalScore: Float,
    val newsScore: Float,
    val overallScore: Float,
    val keyLevels: List<PriceLevel>,
    val newsItems: List<NewsItem>,
    val timestamp: Long
)

@Serializable
enum class TrendDirection {
    BULLISH, BEARISH, SIDEWAYS, UNKNOWN
}

@Serializable
enum class MarketSentiment {
    EXTREMELY_BULLISH, BULLISH, NEUTRAL, BEARISH, EXTREMELY_BEARISH
}

@Serializable
data class PriceLevel(
    val price: Double,
    val type: LevelType,
    val strength: Float, // 0.0 to 1.0
    val description: String
)

@Serializable
enum class LevelType {
    SUPPORT, RESISTANCE, PIVOT
}

@Serializable
data class NewsItem(
    val id: String,
    val title: String,
    val content: String,
    val source: String,
    val timestamp: Long,
    val impact: NewsImpact,
    val relatedSymbols: List<String>,
    val sentiment: Float // -1.0 (negative) to 1.0 (positive)
)

@Serializable
enum class NewsImpact {
    HIGH, MEDIUM, LOW
}