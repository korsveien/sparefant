package no.sparefant

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.responseObject
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.flatMap
import com.github.kittinunf.result.map
import io.javalin.Javalin
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}
val customerId = System.getenv("SBANKEN_ACCOUNT_ID")

fun main(args: Array<String>) {

    val accountInfo = fetchToken()
            .flatMap { fetchAccountId(it) }
            .map { fetchAccountInfo(it) }
            .fold(
                    { it.get() },
                    { err ->
                        logger.error { "Could not retrieve account info: ${err.exception}" }
                        System.exit(1)
                    }
            )

    logger.debug { "accountInfo $accountInfo" }
    val port = System.getenv("PORT")?.toInt() ?: 3000
    val app = Javalin.start(port)
    app.get("/") { it.result("Hello") }
}

private fun fetchToken(): Result<Token, FuelError> {
    val clientId = System.getenv("SBANKEN_CLIENT_ID").let { URLEncoder.encode(it, StandardCharsets.UTF_8) }
    val secret = System.getenv("SBANKEN_SECRET").let { URLEncoder.encode(it, StandardCharsets.UTF_8) }

    logger.debug { "clientId: $clientId" }
    logger.debug { "secret: $secret" }

    val (_, _, result) = "https://api.sbanken.no/identityserver/connect/token"
            .httpPost()
            .header("Content-Type" to "application/x-www-form-urlencoded")
            .header("Accept" to "application/json")
            .authenticate(clientId, secret)
            .body("grant_type=client_credentials", Charsets.UTF_8)
            .also { logger.debug { it } }
            .responseObject<Token>()
            .also { logger.debug { it } }

    return result
}

private fun fetchAccountId(token: Token): Result<Pair<Token, Accounts>, FuelError> {
    val (_, _, result) = "https://api.sbanken.no/bank/api/v1/accounts/"
            .httpGet()
            .header("Authorization" to "${token.tokenType} ${token.accessToken}")
            .header("customerId" to customerId)
            .header("Accept" to "application/json")
            .also { logger.debug { it } }
            .responseObject<Accounts>()
            .also { logger.debug { it } }

    return result.map { Pair(token, it) }

}

private fun fetchAccountInfo(credentials: Pair<Token, Accounts>): Result<String, FuelError> {
    val token = credentials.first
    val accountId = credentials.second.items.first().accountId
    val (_, _, result) = "https://api.sbanken.no/bank/api/v1/accounts/${accountId}"
            .httpGet()
            .header("Accept" to "application/json")
            .header("Authorization" to "${token.tokenType} ${token.accessToken}")
            .header("customerId" to customerId)
            .also { logger.debug { it } }
            .responseString()
            .also { logger.debug { it } }

    return result
}
