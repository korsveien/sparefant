package no.sparefant

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.responseObject
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.KotlinJsonAdapterFactory
import com.squareup.moshi.Moshi
import io.javalin.Javalin
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}
val customerId = System.getenv("SBANKEN_ACCOUNT_ID")
        ?: throw IllegalStateException("Cannot find SBANKEN_ACCOUNT_ID environment value")

val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()!!
val adapter = moshi.adapter(SparefantResponse::class.java)!!
val port = System.getenv("PORT")?.toInt() ?: 3000

fun main(args: Array<String>) {
    val app = Javalin.start(port)

    FuelManager.instance.timeoutInMillisecond = 5000
    FuelManager.instance.timeoutReadInMillisecond = 2000
    FuelManager.instance.addRequestInterceptor { { it.also { logger.debug { it } } } }
    FuelManager.instance.addResponseInterceptor { { _, res -> res.also { logger.debug { res } } } }

    fetchToken()
            .flatMap { fetchAccountId(it) }
            .map { fetchAccountInfo(it) }
            .fold(
                    {
                        handleGet(it, app, adapter)
                    },
                    { err ->
                        logger.error { "Could not retrieve account info: ${err.exception}" }
                        System.exit(1)
                    }
            )
}

private fun handleGet(it: Result<AccountInfo, FuelError>, app: Javalin, adapter: JsonAdapter<SparefantResponse>) {
    val available = it.get().item.available
    val response = SparefantResponse(available)
    app.get("/") { it.result(adapter.toJson(response)) }
}

private fun fetchToken(): Result<Token, FuelError> {

    val clientId = System.getenv("SBANKEN_CLIENT_ID").let { URLEncoder.encode(it, StandardCharsets.UTF_8) }
            ?: throw IllegalStateException("Cannot find SBANKEN_CLIENT_ID environment value")

    val secret = System.getenv("SBANKEN_SECRET").let { URLEncoder.encode(it, StandardCharsets.UTF_8) }
            ?: throw IllegalStateException("Cannot find SBANKEN_SECRET environment value")

    logger.debug { "clientId: $clientId" }
    logger.debug { "secret: $secret" }

    val (_, _, result) = "https://api.sbanken.no/identityserver/connect/token"
            .httpPost()
            .header("Content-Type" to "application/x-www-form-urlencoded")
            .header("Accept" to "application/json")
            .authenticate(clientId, secret)
            .body("grant_type=client_credentials", Charsets.UTF_8)
            .responseObject<Token>()

    return result
}

private fun fetchAccountId(token: Token): Result<Pair<Token, Accounts>, FuelError> {
    val (_, _, result) = "https://api.sbanken.no/bank/api/v1/accounts/"
            .httpGet()
            .header("Authorization" to "${token.tokenType} ${token.accessToken}")
            .header("customerId" to customerId)
            .header("Accept" to "application/json")
            .responseObject<Accounts>()

    return result.map { Pair(token, it) }
}

private fun fetchAccountInfo(credentials: Pair<Token, Accounts>): Result<AccountInfo, FuelError> {
    val token = credentials.first
    val accountId = credentials.second.items.first().accountId
    val (_, _, result) = "https://api.sbanken.no/bank/api/v1/accounts/$accountId"
            .httpGet()
            .header("Accept" to "application/json")
            .header("Authorization" to "${token.tokenType} ${token.accessToken}")
            .header("customerId" to customerId)
            .responseObject<AccountInfo>()

    return result
}
