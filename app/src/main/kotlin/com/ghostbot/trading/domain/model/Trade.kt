package com.ghostbot.trading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Trade(
    val id: String,
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
    val comment: String? = null
)

@Serializable
enum class TradeType {
    BUY, SELL
}

@Serializable
enum class TradeStatus {
    PENDING, OPEN, CLOSED, CANCELLED
}

@Serializable
data class TradeRequest(
    val symbol: String,
    val type: TradeType,
    val volume: Double,
    val price: Double? = null, // null for market orders
    val stopLoss: Double? = null,
    val takeProfit: Double? = null,
    val comment: String? = null
)

@Serializable
data class MarketPrice(
    val symbol: String,
    val bid: Double,
    val ask: Double,
    val spread: Double,
    val timestamp: Long,
    val volume: Long? = null
)

@Serializable
data class Symbol(
    val name: String,
    val displayName: String,
    val category: String,
    val digits: Int,
    val contractSize: Double,
    val tickSize: Double,
    val tickValue: Double,
    val minVolume: Double,
    val maxVolume: Double,
    val stepVolume: Double,
    val isActive: Boolean = true
)