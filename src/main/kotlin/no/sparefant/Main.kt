package no.sparefant

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.responseObject
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.javalin.Javalin
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {

    fetchToken()
            .map { fetchAccountInfo(it) }
            .fold(
                    { succ -> println(succ.get()) },
                    { err -> logger.warn { err.exception }}
            )

    val port = System.getenv("PORT")?.toInt() ?: 3000
    val app = Javalin.start(port)
    app.get("/") { it.result("Hello") }
}

private fun fetchToken(): Result<Token, FuelError> {
    val clientID = System.getenv("SBANKEN_CLIENT_ID")
    val secret = System.getenv("SBANKEN_SECRET")

    val (_, _, result) = "https://api.sbanken.no/identityserver/connect/token"
            .httpPost()
            .header("Content-Type" to "application/x-www-form-urlencoded")
            .authenticate(clientID, secret)
            .body("grant_type=client_credentials", Charsets.UTF_8)
            .also { logger.debug { it } }
            .responseObject<Token>()
            .also { logger.debug { it } }

    return result
}

private fun fetchAccountInfo(token: Token): Result<AccountInfo, FuelError> {
    val accountId = System.getenv("SBANKEN_ACCOUNT_ID")
    val (_, _, result) = "https://api.sbanken.no/bank/api/v1/accounts/$accountId"
            .httpGet()
            .header("Accept" to "application/json")
            .header("Authorization" to "${token.tokenType} ${token.accessToken}")
            .also { logger.debug { it } }
            .responseObject<AccountInfo>()
            .also { logger.debug { it } }

    return result
}
