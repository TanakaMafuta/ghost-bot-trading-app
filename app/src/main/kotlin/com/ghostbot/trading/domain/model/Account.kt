package com.ghostbot.trading.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val accountId: String,
    val accountType: AccountType,
    val balance: Double,
    val equity: Double,
    val margin: Double,
    val freeMargin: Double,
    val marginLevel: Double,
    val currency: String,
    val leverage: Int,
    val server: String,
    val isConnected: Boolean = false,
    val lastUpdate: Long = System.currentTimeMillis()
)

@Serializable
enum class AccountType {
    DEMO, REAL
}

@Serializable
data class LoginCredentials(
    val loginId: String,
    val password: String,
    val accountType: AccountType,
    val server: String = "Deriv-Demo"
)

@Serializable
data class AuthToken(
    val token: String,
    val refreshToken: String,
    val expiresAt: Long,
    val accountId: String
)