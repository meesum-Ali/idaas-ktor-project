package com.idaas

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(OpenAPIGen) {
        info {
            title = "IDaaS User API"
            version = "1.0.0"
            description = "OpenAPI documentation for the User API."
        }
    }
    routing {
        get("/") {
            call.respondText("Welcome to Identity-as-a-Service API!")
        }
        openAPI(path = "/openapi")
        swaggerUI(path = "/swagger", swaggerFile = "/openapi")
    }
}