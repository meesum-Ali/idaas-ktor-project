package com.idaas

import com.idaas.user.api.userApi
import com.idaas.user.api.UserRequest
import com.idaas.user.application.RegisterUserService
import com.idaas.user.application.UpdateUserProfileService
import com.idaas.user.application.DeleteUserService
import com.idaas.user.application.LoginUserService
import com.idaas.jwt.JwtService // Import JwtService
import com.idaas.user.infrastructure.DbUserRepository
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.exposed.sql.Database

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { json() }
    install(OpenAPIGen) {
        info {
            title = "IDaaS User API"
            version = "1.0.0"
            description = "OpenAPI documentation for the User API."
        }
    }
    // Setup DB and repository
    val db = Database.connect(
        url = "jdbc:sqlite:build/prod-db.sqlite",
        driver = "org.sqlite.JDBC"
    )
    val userRepo = DbUserRepository(db)
    val registerUserService = RegisterUserService(userRepo)
    val updateUserProfileService = UpdateUserProfileService(userRepo)
    val deleteUserService = DeleteUserService(userRepo)
    val loginUserService = LoginUserService(userRepo)
    val jwtService = JwtService() // Instantiate JwtService

    routing {
        get("/") {
            call.respondText("Welcome to Identity-as-a-Service API!")
        }
        route("/api") {
            // Pass LoginUserService and JwtService to userApi
            userApi(
                registerUserService,
                updateUserProfileService,
                deleteUserService,
                loginUserService,
                jwtService // Pass JwtService
            )
        }
        openAPI(path = "/openapi")
        swaggerUI(path = "/swagger", swaggerFile = "/openapi")
    }
}