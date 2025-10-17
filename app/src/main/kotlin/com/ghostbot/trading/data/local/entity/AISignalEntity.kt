package com.ghostbot.trading.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghostbot.trading.domain.model.AISource
import com.ghostbot.trading.domain.model.SignalType

@Entity(tableName = "ai_signals")
data class AISignalEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val signal: SignalType,
    val confidence: Float,
    val recommendation: String,
    val entryPrice: Double? = null,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val timeframe: String,
    val timestamp: Long,
    val source: AISource,
    val analysis: String,
    val metadata: String = "{}" // JSON string
)