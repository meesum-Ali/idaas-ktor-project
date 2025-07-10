package com.idaas.user.api

import com.idaas.user.application.RegisterUserService
import com.idaas.user.application.UpdateUserProfileService
import com.idaas.user.application.DeleteUserService
import com.idaas.user.application.LoginUserService
import com.idaas.user.application.AuthenticationException
import com.idaas.jwt.JwtService // Import JwtService
import com.idaas.user.domain.User
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

@Serializable
data class UserRegistrationRequest(val email: String, val name: String, val phone: String, val password: String)

@Serializable
data class UserLoginRequest(val email: String, val password: String)

@Serializable
data class UserUpdateRequest(val name: String, val phone: String) // Password updates should be a separate, secure flow

fun Route.userApi(
    registerUserService: RegisterUserService,
    updateUserProfileService: UpdateUserProfileService,
    deleteUserService: DeleteUserService,
    loginUserService: LoginUserService,
    jwtService: JwtService // Add JwtService parameter
) {
    post("/users/register") {
        val req = call.receive<UserRegistrationRequest>()
        try {
            val user = registerUserService.register(req.email, req.name, req.phone, req.password)
            // Consider returning a UserResponse DTO that omits sensitive fields like hashedPassword
            call.respond(HttpStatusCode.Created, user)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }
    }

    post("/users/login") {
        val req = call.receive<UserLoginRequest>()
        try {
            val user = loginUserService.login(req.email, req.password)
            val token = jwtService.generateToken(user)
            call.respond(HttpStatusCode.OK, mapOf("token" to token))
        } catch (e: AuthenticationException) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to e.message))
        } catch (e: Exception) {
            // Generic error handler for unexpected issues
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred."))
        }
    }

    put("/users/{id}") {
        val id = call.parameters["id"]!!
        val req = call.receive<UserUpdateRequest>()
        try {
            val updatedUser = updateUserProfileService.updateProfile(id, req.name, req.phone)
            call.respond(HttpStatusCode.OK, updatedUser)
        } catch (e: IllegalArgumentException) {
            // This exception is thrown by UpdateUserProfileService if user not found
            call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
        } catch (e: Exception) {
            // Generic error handler for unexpected issues
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred during update."))
        }
    }
    delete("/users/{id}") {
        val id = call.parameters["id"]!!
        try {
            deleteUserService.deleteById(id)
            call.respond(HttpStatusCode.NoContent)
        } catch (e: IllegalArgumentException) {
            // This exception is thrown by DeleteUserService if user not found
            call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
        } catch (e: Exception) {
            // Generic error handler for unexpected issues
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred during deletion."))
        }
    }
}