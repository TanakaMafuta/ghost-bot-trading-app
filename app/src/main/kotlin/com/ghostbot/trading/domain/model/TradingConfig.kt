package com.ghostbot.trading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TradingConfig(
    val id: String = "default",
    val lotSize: Double = 0.01,
    val leverage: Int = 100,
    val maxDrawdown: Float = 0.05f, // 5%
    val stopLossPercent: Float = 0.02f, // 2%
    val takeProfitPercent: Float = 0.04f, // 4%
    val timeframe: Timeframe = Timeframe.M15,
    val riskLevel: RiskLevel = RiskLevel.MODERATE,
    val aiTradingEnabled: Boolean = false,
    val maxOpenTrades: Int = 5,
    val maxDailyTrades: Int = 20,
    val tradingHours: TradingHours,
    val allowedSymbols: List<String> = emptyList(),
    val emergencyStopEnabled: Boolean = true,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Serializable
enum class Timeframe {
    M1, M5, M15, M30, H1, H4, D1, W1, MN1
}

@Serializable
enum class RiskLevel {
    CONSERVATIVE, MODERATE, AGGRESSIVE
}

@Serializable
data class TradingHours(
    val startHour: Int = 0, // 24-hour format
    val endHour: Int = 23,
    val tradingDays: List<Int> = listOf(1, 2, 3, 4, 5), // Monday to Friday
    val timezone: String = "UTC"
)

@Serializable
data class RiskManagement(
    val maxRiskPerTrade: Float = 0.02f, // 2% of balance
    val maxDailyRisk: Float = 0.10f, // 10% of balance
    val correlationLimit: Float = 0.7f, // Max correlation between open trades
    val marginCallLevel: Float = 0.5f, // 50% margin level
    val autoCloseLevel: Float = 0.2f // 20% margin level
)