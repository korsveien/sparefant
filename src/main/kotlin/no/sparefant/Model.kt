package no.sparefant

import com.squareup.moshi.Json

data class Token(
        @Json(name = "access_token") val accessToken: String,
        @Json(name="expires_in") val expiresIn: Int,
        @Json(name="token_type") val tokenType: String
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