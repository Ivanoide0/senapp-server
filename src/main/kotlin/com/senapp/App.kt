package com.senapp

import com.senapp.db.Postgres
import com.senapp.model.*
import com.senapp.repo.GrammarRepo
import com.senapp.svc.Interpreter

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

// Plugins
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.plugins.defaultheaders.*

// 👇 OJO: es "callloging" (una sola g), así lo nombra Ktor 2
import io.ktor.server.plugins.callloging.*

import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.http.HttpHeaders
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.event.Level

// App.kt
fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(
        Netty,
        host = "0.0.0.0",   // <- IMPORTANTE
        port = port
    ) { module() }.start(wait = true)
}


@Serializable
data class HealthResponse(val ok: Boolean = true, val service: String = "senapp-interpret")

fun Application.module() {
    install(DefaultHeaders)
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(mapOf("error" to (cause.message ?: "internal error")))
        }
    }

    // 👇 Logging (requiere import io.ktor.server.plugins.callloging.* y la dependencia)
    install(CallLogging) { level = Level.INFO }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }

    install(CORS) {
        anyHost() // en prod restringe dominios
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-API-Key")
    }

    // Inicia pool de BD (lee DB_URL de tus Environment variables)
    Postgres.init(environment)

    routing {
        get("/") { call.respond(HealthResponse()) }

        post("/interpret") {
            val req = call.receive<InterpretRequest>()
            val repo = GrammarRepo(Postgres.ds!!)
            val interpreter = Interpreter(repo)
            val res = interpreter.interpret(req)
            call.respond(res)
        }
    }
}
