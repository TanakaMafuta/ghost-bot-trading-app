package com.ghostbot.trading.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthRequest(
    val grant_type: String = "password",
    val username: String,
    val password: String,
    val scope: String = "read,trade"
)

@Serializable
data class AuthResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val token_type: String = "Bearer",
    val scope: String
)

@Serializable
data class RefreshTokenRequest(
    val grant_type: String = "refresh_token",
    val refresh_token: String
)

@Serializable
data class AccountStatusResponse(
    val account_status: AccountStatusData
)

@Serializable
data class AccountStatusData(
    val authentication: AuthenticationStatus,
    val status: List<String>,
    val p2p_status: String,
    val risk_classification: String,
    val currency_config: Map<String, CurrencyConfig>
)

@Serializable
data class AuthenticationStatus(
    val needs_verification: List<String>? = null,
    val document_status: String? = null,
    val identity_status: String? = null
)

@Serializable
data class CurrencyConfig(
    val fractional_digits: Int,
    val is_deposit_suspended: Int,
    val is_withdrawal_suspended: Int,
    val stake_default: Double?
)

@Serializable
data class PortfolioResponse(
    val portfolio: PortfolioData
)

@Serializable
data class PortfolioData(
    val contracts: List<ContractData>
)

@Serializable
data class ContractData(
    val app_id: Int,
    val buy_price: Double,
    val contract_id: Long,
    val contract_type: String,
    val currency: String,
    val date_start: Long,
    val expiry_time: Long,
    val longcode: String,
    val payout: Double,
    val purchase_time: Long,
    val shortcode: String,
    val symbol: String,
    val transaction_id: Long
)

@Serializable
data class StatementResponse(
    val statement: StatementData
)

@Serializable
data class StatementData(
    val count: Int,
    val transactions: List<TransactionData>
)

@Serializable
data class TransactionData(
    val transaction_id: Long,
    val contract_id: Long?,
    val purchase_time: Long,
    val action_type: String,
    val amount: Double,
    val balance_after: Double,
    val shortcode: String?,
    val transaction_time: Long
)

@Serializable
data class TradeRequest(
    val contract_type: String,
    val currency: String,
    val amount: Double,
    val symbol: String,
    val duration: Int,
    val duration_unit: String,
    val basis: String = "stake"
)

@Serializable
data class CloseTradeRequest(
    val sell: String // contract_id
)

@Serializable
data class TradeResponse(
    val buy: BuyData? = null,
    val sell: SellData? = null
)

@Serializable
data class BuyData(
    val balance_after: Double,
    val buy_price: Double,
    val contract_id: Long,
    val longcode: String,
    val payout: Double,
    val purchase_time: Long,
    val shortcode: String,
    val start_time: Long,
    val transaction_id: Long
)

@Serializable
data class SellData(
    val balance_after: Double,
    val contract_id: Long,
    val reference_id: Long,
    val sell_price: Double,
    val sell_time: Long,
    val transaction_id: Long
)

@Serializable
data class SymbolsResponse(
    val active_symbols: List<SymbolData>
)

@Serializable
data class SymbolData(
    val symbol: String,
    val display_name: String,
    val exchange_is_open: Int,
    val is_trading_suspended: Int,
    val market: String,
    val market_display_name: String,
    val pip: Double,
    val submarket: String,
    val submarket_display_name: String
)

@Serializable
data class TicksHistoryResponse(
    val candles: List<CandleData>? = null,
    val history: HistoryData? = null
)

@Serializable
data class CandlesResponse(
    val candles: List<CandleData>
)

@Serializable
data class CandleData(
    val close: Double,
    val epoch: Long,
    val high: Double,
    val low: Double,
    val open: Double
)

@Serializable
data class HistoryData(
    val prices: List<Double>,
    val times: List<Long>
)

@Serializable
data class ProposalRequest(
    val proposal: Int = 1,
    val amount: Double,
    val basis: String = "stake",
    val contract_type: String,
    val currency: String,
    val duration: Int,
    val duration_unit: String,
    val symbol: String
)

@Serializable
data class ProposalResponse(
    val proposal: ProposalData
)

@Serializable
data class ProposalData(
    val ask_price: Double,
    val date_start: Long,
    val display_value: String,
    val id: String,
    val longcode: String,
    val payout: Double,
    val spot: Double,
    val spot_time: Long
)

@Serializable
data class PingResponse(
    val ping: String
)