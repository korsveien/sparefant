package no.sparefant

import com.github.kittinunf.fuel.httpPost
import io.javalin.Javalin
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    fetchToken()
    val port = System.getenv("PORT")?.toInt() ?: 3000
    logger.info { "PORT is $port" }
    val app = Javalin.start(port)
    app.get("/") { it.result("Hello") }
}

private fun fetchToken() {
    val clientID = System.getenv("SBANKEN_CLIENT_ID")
    val secret = System.getenv("SBANKEN_SECRET")

    val (_, response, _) = "https://api.sbanken.no/identityserver/connect/token"
            .httpPost()
            .header("Content-Type" to "application/x-www-form-urlencoded")
            .authenticate(clientID, secret)
            .body("grant_type=client_credentials", Charsets.UTF_8)
            .responseString()

    println(response)
}
