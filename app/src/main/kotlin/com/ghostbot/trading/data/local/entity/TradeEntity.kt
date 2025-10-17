package com.ghostbot.trading.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ghostbot.trading.domain.model.TradeStatus
import com.ghostbot.trading.domain.model.TradeType

@Entity(tableName = "trades")
data class TradeEntity(
    @PrimaryKey val id: String,
    val symbol: String,
    val type: TradeType,
    val volume: Double,
    val openPrice: Double,
    val closePrice: Double? = null,
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val openTime: Long,
    val closeTime: Long? = null,
    val profit: Double = 0.0,
    val commission: Double = 0.0,
    val swap: Double = 0.0,
    val status: TradeStatus,
    val comment: String? = null,
    val accountId: String
)