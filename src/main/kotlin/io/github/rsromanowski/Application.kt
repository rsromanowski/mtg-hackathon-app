package io.github.rsromanowski

import io.github.rsromanowski.plugins.*
import io.github.rsromanowski.scryfall.MagicCardClient
import io.github.rsromanowski.scryfall.ScryfallClient
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.runBlocking

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureRouting()

//    DatabaseSingleton.createHikariDataSource(
//        environment.config.propertyOrNull("DATABASE_URL")?.getString()
//            ?: error("DATABASE_URL not configured")
//    ).let {
//        doDatabaseThings(it)
//    }

//    val cardClient: MagicCardClient = ScryfallClient(log)
//    runBlocking { cardClient.getMkmCards() }
}
