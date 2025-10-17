package com.ghostbot.trading.data.remote.dto.openai

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String = "gpt-4",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    val max_tokens: Int = 1000,
    val top_p: Double = 1.0,
    val frequency_penalty: Double = 0.0,
    val presence_penalty: Double = 0.0,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val object: String,
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage,
    val system_fingerprint: String?
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    val finish_reason: String
)

@Serializable
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class EmbeddingsRequest(
    val model: String = "text-embedding-ada-002",
    val input: List<String>,
    val encoding_format: String = "float"
)

@Serializable
data class EmbeddingsResponse(
    val object: String,
    val data: List<EmbeddingData>,
    val model: String,
    val usage: EmbeddingUsage
)

@Serializable
data class EmbeddingData(
    val object: String,
    val index: Int,
    val embedding: List<Double>
)

@Serializable
data class EmbeddingUsage(
    val prompt_tokens: Int,
    val total_tokens: Int
)

@Serializable
data class ModelsResponse(
    val object: String,
    val data: List<ModelData>
)

@Serializable
data class ModelData(
    val id: String,
    val object: String,
    val created: Long,
    val owned_by: String
)