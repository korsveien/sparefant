package no.sparefant

import com.squareup.moshi.Json

data class Token(
        @Json(name = "access_token") val accessToken: String,
        @Json(name="expires_in") val expiresIn: Int,
        @Json(name="token_type") val tokenType: String
)

data class Accounts(
    @Json(name = "availableItems") val availableItems: Int,
    @Json(name = "items") val items: List<AccountsItem>,
    @Json(name = "errorType") val errorType: String?,
    @Json(name = "isError") val isError: Boolean,
    @Json(name = "errorMessage") val errorMessage: String?,
    @Json(name = "traceId") val traceId: String?
)

data class AccountsItem(
    @Json(name = "accountId") val accountId: String,
    @Json(name = "accountNumber") val accountNumber: String,
    @Json(name = "ownerCustomerId") val ownerCustomerId: String,
    @Json(name = "name") val name: String,
    @Json(name = "accountType") val accountType: String,
    @Json(name = "available") val available: Double,
    @Json(name = "balance") val balance: Double,
    @Json(name = "creditLimit") val creditLimit: Double
)

data class AccountInfo(
    val availableItems: Int,
    val items: List<Item>,
    val errorType: String?,
    val isError: Boolean,
    val errorMessage: String?,
    val traceId: String?
)

data class Item(
    val accountNumber: String,
    val customerId: String,
    val ownerCustomerId: String,
    val name: String,
    val accountType: String,
    val available: Double,
    val balance: Double,
    val creditLimit: Double,
    val defaultAccount: Boolean
)