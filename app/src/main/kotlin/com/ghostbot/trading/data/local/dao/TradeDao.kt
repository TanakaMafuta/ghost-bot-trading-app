package com.ghostbot.trading.data.local.dao

import androidx.room.*
import com.ghostbot.trading.data.local.entity.TradeEntity
import com.ghostbot.trading.domain.model.TradeStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {
    
    @Query("SELECT * FROM trades WHERE accountId = :accountId ORDER BY openTime DESC")
    fun getTradesByAccount(accountId: String): Flow<List<TradeEntity>>
    
    @Query("SELECT * FROM trades WHERE accountId = :accountId AND status = :status")
    fun getTradesByStatus(accountId: String, status: TradeStatus): Flow<List<TradeEntity>>
    
    @Query("SELECT * FROM trades WHERE id = :tradeId")
    suspend fun getTradeById(tradeId: String): TradeEntity?
    
    @Query("SELECT * FROM trades WHERE accountId = :accountId AND openTime >= :startTime AND openTime <= :endTime")
    fun getTradesInDateRange(accountId: String, startTime: Long, endTime: Long): Flow<List<TradeEntity>>
    
    @Query("SELECT SUM(profit) FROM trades WHERE accountId = :accountId AND status = 'CLOSED'")
    suspend fun getTotalProfit(accountId: String): Double?
    
    @Query("SELECT COUNT(*) FROM trades WHERE accountId = :accountId AND status = 'OPEN'")
    suspend fun getOpenTradesCount(accountId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<TradeEntity>)
    
    @Update
    suspend fun updateTrade(trade: TradeEntity)
    
    @Delete
    suspend fun deleteTrade(trade: TradeEntity)
    
    @Query("DELETE FROM trades WHERE accountId = :accountId")
    suspend fun deleteTradesByAccount(accountId: String)
}