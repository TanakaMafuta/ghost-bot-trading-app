package com.ghostbot.trading.data.remote.api

import com.ghostbot.trading.data.remote.dto.perplexity.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Perplexity API service for real-time market research and news analysis
 */
interface PerplexityApiService {
    
    @POST("chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: PerplexityRequest
    ): Response<PerplexityResponse>
    
    @POST("search")
    suspend fun searchMarketNews(
        @Header("Authorization") apiKey: String,
        @Body request: MarketSearchRequest
    ): Response<MarketSearchResponse>
}