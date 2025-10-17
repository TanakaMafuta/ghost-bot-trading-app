package com.ghostbot.trading.data.remote.api

import com.ghostbot.trading.data.remote.dto.openai.*
import retrofit2.Response
import retrofit2.http.*

/**
 * OpenAI API service for market analysis and trading insights
 */
interface OpenAIApiService {
    
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
    
    @POST("embeddings")
    suspend fun getEmbeddings(
        @Header("Authorization") apiKey: String,
        @Body request: EmbeddingsRequest
    ): Response<EmbeddingsResponse>
    
    @GET("models")
    suspend fun getModels(
        @Header("Authorization") apiKey: String
    ): Response<ModelsResponse>
}