package com.ghostbot.trading.data.remote.dto.perplexity

import kotlinx.serialization.Serializable

@Serializable
data class PerplexityRequest(
    val model: String = "llama-3.1-sonar-small-128k-online",
    val messages: List<PerplexityMessage>,
    val max_tokens: Int = 1000,
    val temperature: Double = 0.2,
    val top_p: Double = 0.9,
    val search_domain_filter: List<String>? = null,
    val return_images: Boolean = false,
    val return_related_questions: Boolean = true,
    val search_recency_filter: String? = "month",
    val top_k: Int = 0,
    val stream: Boolean = false,
    val presence_penalty: Double = 0.0,
    val frequency_penalty: Double = 1.0
)

@Serializable
data class PerplexityMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class PerplexityResponse(
    val id: String,
    val model: String,
    val created: Long,
    val usage: PerplexityUsage,
    val object: String,
    val choices: List<PerplexityChoice>
)

@Serializable
data class PerplexityChoice(
    val index: Int,
    val finish_reason: String,
    val message: PerplexityMessage,
    val delta: PerplexityDelta? = null
)

@Serializable
data class PerplexityDelta(
    val role: String? = null,
    val content: String? = null
)

@Serializable
data class PerplexityUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class MarketSearchRequest(
    val query: String,
    val search_type: String = "news",
    val count: Int = 10,
    val market: String? = null,
    val timeframe: String = "24h",
    val language: String = "en"
)

@Serializable
data class MarketSearchResponse(
    val query: String,
    val results: List<MarketSearchResult>,
    val total_results: Int,
    val search_time: Double
)

@Serializable
data class MarketSearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val published_date: String,
    val source: String,
    val relevance_score: Double,
    val sentiment_score: Double? = null
)