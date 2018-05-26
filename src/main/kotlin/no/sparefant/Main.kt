package no.sparefant

import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.moshi.responseObject
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.map
import io.javalin.Javalin
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {

    val accountInfo = fetchToken()
            .map { fetchAccountInfo(it) }
            .fold(
                    { it.get() },
                    { err ->
                        logger.error { "Could not retrieve account info: ${err.exception}" }
                        System.exit(1)
                    }
            )

    val port = System.getenv("PORT")?.toInt() ?: 3000
    val app = Javalin.start(port)

    app.get("/") { it.result("Hello") }
}

private fun fetchToken(): Result<Token, FuelError> {
    val clientID = System.getenv("SBANKEN_CLIENT_ID").let { URLEncoder.encode(it, StandardCharsets.UTF_8) }
    val secret = System.getenv("SBANKEN_SECRET").let { URLEncoder.encode(it, StandardCharsets.UTF_8) }

    logger.debug { "clientID: $clientID" }
    logger.debug { "secret: $secret" }

    val (_, _, result) = "https://api.sbanken.no/identityserver/connect/token"
            .httpPost()
            .header("Content-Type" to "application/x-www-form-urlencoded")
            .header("Accept" to "application/json")
            .authenticate(clientID, secret)
            .body("grant_type=client_credentials", Charsets.UTF_8)
            .also { logger.debug { it } }
            .responseObject<Token>()
            .also { logger.debug { it } }

    return result
}

private fun fetchAccountInfo(token: Token): Result<String, FuelError> {
    val customerId = System.getenv("SBANKEN_ACCOUNT_ID")
    val (_, _, result) = "https://api.sbanken.no/bank/api/v1/accounts/$customerId"
            .httpGet()
            .header("Accept" to "application/json")
            .header("Authorization" to "${token.tokenType} ${token.accessToken}")
            .header("customerId" to customerId)
            .also { logger.debug { it } }
            .responseString()
            .also { logger.debug { it } }

    return result
}
