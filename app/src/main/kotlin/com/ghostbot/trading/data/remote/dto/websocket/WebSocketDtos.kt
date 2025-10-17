package com.ghostbot.trading.data.remote.dto.websocket

import kotlinx.serialization.Serializable

// Request DTOs
@Serializable
data class AuthorizeRequest(
    val authorize: String,
    val req_id: Int
)

@Serializable
data class TicksRequest(
    val ticks: String,
    val subscribe: Int = 1,
    val req_id: Int
)

@Serializable
data class CandlesRequest(
    val ticks_history: String,
    val granularity: Int = 60,
    val subscribe: Int = 1,
    val req_id: Int
)

@Serializable
data class PortfolioRequest(
    val portfolio: Int = 1,
    val subscribe: Int = 1,
    val req_id: Int
)

@Serializable
data class BalanceRequest(
    val balance: Int = 1,
    val subscribe: Int = 1,
    val req_id: Int
)

@Serializable
data class ProposalOpenContractRequest(
    val proposal_open_contract: Int = 1,
    val subscribe: Int = 1,
    val req_id: Int
)

@Serializable
data class BuyRequest(
    val buy: String, // proposal_id
    val price: Double,
    val req_id: Int
)

@Serializable
data class SellRequest(
    val sell: String, // contract_id
    val price: Double,
    val req_id: Int
)

// Response DTOs
@Serializable
data class AuthorizeResponse(
    val authorize: AuthorizeData?,
    val error: ErrorData? = null,
    val req_id: Int
)

@Serializable
data class AuthorizeData(
    val account_list: List<AccountData>,
    val balance: Double,
    val country: String,
    val currency: String,
    val email: String,
    val fullname: String,
    val is_virtual: Int,
    val landing_company_fullname: String,
    val landing_company_name: String,
    val local_currencies: Map<String, LocalCurrencyData>,
    val loginid: String,
    val scopes: List<String>
)

@Serializable
data class AccountData(
    val account_type: String,
    val currency: String,
    val is_disabled: Int,
    val is_virtual: Int,
    val landing_company_name: String,
    val loginid: String
)

@Serializable
data class LocalCurrencyData(
    val fractional_digits: Int
)

@Serializable
data class BuyResponse(
    val buy: BuyResponseData,
    val error: ErrorData? = null,
    val req_id: Int
)

@Serializable
data class BuyResponseData(
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
data class SellResponse(
    val sell: SellResponseData,
    val error: ErrorData? = null,
    val req_id: Int
)

@Serializable
data class SellResponseData(
    val balance_after: Double,
    val contract_id: Long,
    val reference_id: Long,
    val sell_price: Double,
    val sell_time: Long,
    val transaction_id: Long
)

@Serializable
data class ErrorData(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

// Stream Data DTOs
@Serializable
data class TickData(
    val tick: TickInfo,
    val subscription: SubscriptionInfo
)

@Serializable
data class TickInfo(
    val ask: Double,
    val bid: Double,
    val epoch: Long,
    val id: String,
    val pip_size: Int,
    val quote: Double,
    val symbol: String
)

@Serializable
data class SubscriptionInfo(
    val id: String
)

@Serializable
data class BalanceData(
    val balance: BalanceInfo,
    val subscription: SubscriptionInfo
)

@Serializable
data class BalanceInfo(
    val balance: Double,
    val currency: String,
    val id: String,
    val loginid: String
)

@Serializable
data class TradeData(
    val portfolio: PortfolioInfo? = null,
    val proposal_open_contract: ProposalOpenContractInfo? = null
)

@Serializable
data class PortfolioInfo(
    val contracts: List<ContractInfo>
)

@Serializable
data class ContractInfo(
    val app_id: Int,
    val buy_price: Double,
    val contract_id: Long,
    val contract_type: String,
    val currency: String,
    val current_spot: Double?,
    val current_spot_display_value: String?,
    val current_spot_time: Long?,
    val date_start: Long,
    val expiry_time: Long,
    val is_expired: Int,
    val is_forward_starting: Int,
    val is_intraday: Int,
    val is_path_dependent: Int,
    val is_settleable: Int,
    val is_sold: Int,
    val is_valid_to_sell: Int,
    val longcode: String,
    val payout: Double,
    val profit: Double,
    val profit_percentage: Double,
    val purchase_time: Long,
    val shortcode: String,
    val symbol: String,
    val transaction_id: Long
)

@Serializable
data class ProposalOpenContractInfo(
    val contract_id: Long,
    val current_spot: Double,
    val current_spot_display_value: String,
    val current_spot_time: Long,
    val display_name: String,
    val is_expired: Int,
    val is_forward_starting: Int,
    val is_intraday: Int,
    val is_path_dependent: Int,
    val is_settleable: Int,
    val is_sold: Int,
    val is_valid_to_sell: Int,
    val profit: Double,
    val profit_percentage: Double,
    val validation_error: String?
)

// Helper data classes
data class BuyProposal(
    val id: String,
    val price: Double,
    val payout: Double,
    val symbol: String,
    val contractType: String
)

data class AccountStatus(
    val status: String,
    val currency: String,
    val balance: Double
)

data class Portfolio(
    val contracts: List<ContractInfo>
)

data class Balance(
    val balance: Double,
    val currency: String
)

data class Symbol(
    val name: String,
    val displayName: String,
    val isActive: Boolean
)

data class TicksHistory(
    val symbol: String,
    val ticks: List<TickInfo>
)