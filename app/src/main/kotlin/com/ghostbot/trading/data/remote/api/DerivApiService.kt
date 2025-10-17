package com.ghostbot.trading.data.remote.api

import com.ghostbot.trading.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Deriv API service interface for REST endpoints
 * WebSocket connections are handled separately
 */
interface DerivApiService {
    
    @POST("oauth2/token")
    suspend fun authenticate(
        @Body request: AuthRequest
    ): Response<AuthResponse>
    
    @POST("oauth2/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>
    
    @GET("account/status")
    suspend fun getAccountStatus(
        @Header("Authorization") token: String
    ): Response<AccountStatusResponse>
    
    @GET("portfolio")
    suspend fun getPortfolio(
        @Header("Authorization") token: String
    ): Response<PortfolioResponse>
    
    @GET("statement")
    suspend fun getStatement(
        @Header("Authorization") token: String,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("limit") limit: Int? = null
    ): Response<StatementResponse>
    
    @POST("buy")
    suspend fun executeTrade(
        @Header("Authorization") token: String,
        @Body request: TradeRequest
    ): Response<TradeResponse>
    
    @POST("sell")
    suspend fun closeTrade(
        @Header("Authorization") token: String,
        @Body request: CloseTradeRequest
    ): Response<TradeResponse>
    
    @GET("active_symbols")
    suspend fun getActiveSymbols(
        @Query("active_symbols") filter: String = "brief"
    ): Response<SymbolsResponse>
    
    @GET("ticks_history")
    suspend fun getTicksHistory(
        @Query("ticks_history") symbol: String,
        @Query("end") endTime: String = "latest",
        @Query("count") count: Int = 1000
    ): Response<TicksHistoryResponse>
    
    @GET("candles")
    suspend fun getCandles(
        @Query("ticks_history") symbol: String,
        @Query("granularity") granularity: Int = 60, // seconds
        @Query("count") count: Int = 1000
    ): Response<CandlesResponse>
    
    @POST("proposal")
    suspend fun getProposal(
        @Body request: ProposalRequest
    ): Response<ProposalResponse>
    
    @GET("ping")
    suspend fun ping(): Response<PingResponse>
}