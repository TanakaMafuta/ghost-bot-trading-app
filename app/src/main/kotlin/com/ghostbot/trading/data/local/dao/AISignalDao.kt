package com.ghostbot.trading.data.local.dao

import androidx.room.*
import com.ghostbot.trading.data.local.entity.AISignalEntity
import com.ghostbot.trading.domain.model.AISource
import com.ghostbot.trading.domain.model.SignalType
import kotlinx.coroutines.flow.Flow

@Dao
interface AISignalDao {
    
    @Query("SELECT * FROM ai_signals ORDER BY timestamp DESC")
    fun getAllSignals(): Flow<List<AISignalEntity>>
    
    @Query("SELECT * FROM ai_signals WHERE symbol = :symbol ORDER BY timestamp DESC")
    fun getSignalsBySymbol(symbol: String): Flow<List<AISignalEntity>>
    
    @Query("SELECT * FROM ai_signals WHERE source = :source ORDER BY timestamp DESC")
    fun getSignalsBySource(source: AISource): Flow<List<AISignalEntity>>
    
    @Query("SELECT * FROM ai_signals WHERE signal = :signalType ORDER BY timestamp DESC")
    fun getSignalsByType(signalType: SignalType): Flow<List<AISignalEntity>>
    
    @Query("SELECT * FROM ai_signals WHERE symbol = :symbol ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestSignalForSymbol(symbol: String): AISignalEntity?
    
    @Query("SELECT * FROM ai_signals WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getRecentSignals(startTime: Long): Flow<List<AISignalEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignal(signal: AISignalEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignals(signals: List<AISignalEntity>)
    
    @Delete
    suspend fun deleteSignal(signal: AISignalEntity)
    
    @Query("DELETE FROM ai_signals WHERE timestamp < :cutoffTime")
    suspend fun deleteOldSignals(cutoffTime: Long)
    
    @Query("DELETE FROM ai_signals")
    suspend fun deleteAllSignals()
}