package com.idaas.user.api

import com.idaas.user.application.RegisterUserService
import com.idaas.user.application.UpdateUserProfileService
import com.idaas.user.application.DeleteUserService
import com.idaas.user.application.LoginUserService
import com.idaas.user.application.AuthenticationException
import com.idaas.auth.UserPrincipal // Import UserPrincipal
import com.idaas.jwt.JwtService
import com.idaas.user.domain.User
import io.ktor.server.application.*
import io.ktor.server.auth.* // For call.principal()
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
    jwtService: JwtService
) {
    // Public routes (no authentication needed)
    post("/users/register") {
        val req = call.receive<UserRegistrationRequest>()
        try {
            val user = registerUserService.register(req.email, req.name, req.phone, req.password)
            call.respond(HttpStatusCode.Created, user) // Consider DTO
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
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred."))
        }
    }

    // Authenticated routes
    authenticate("auth-jwt") {
        put("/users/{id}") {
            val id = call.parameters["id"]!!
            val req = call.receive<UserUpdateRequest>()
            val principal = call.principal<UserPrincipal>()!! // !! is safe here due to authenticate block
            println("User ${principal.id} with roles ${principal.roles} is attempting to update user $id")
            // TODO: Add authorization logic: e.g., if (principal.id != id && !"ADMIN".equals(principal.roles.firstOrNull(), ignoreCase = true)) { respond(Forbidden) }

            // Further authorization can be added here: e.g., check if principal.id == id or principal has ADMIN role
            try {
                val updatedUser = updateUserProfileService.updateProfile(id, req.name, req.phone)
                call.respond(HttpStatusCode.OK, updatedUser)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred during update."))
            }
        }

        delete("/users/{id}") {
            val id = call.parameters["id"]!!
            val principal = call.principal<UserPrincipal>()!!
            println("User ${principal.id} with roles ${principal.roles} is attempting to delete user $id")
            // TODO: Add authorization logic here as well

            // Further authorization can be added here
            try {
                deleteUserService.deleteById(id)
                call.respond(HttpStatusCode.NoContent)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to e.message))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "An unexpected error occurred during deletion."))
            }
        }

        get("/users/me") {
            val principal = call.principal<UserPrincipal>()
                ?: return@get call.respond(HttpStatusCode.InternalServerError, "User principal not found after authentication.") // Should not happen

            // For now, just return the principal.
            // In a real app, you might fetch the full User object from DB using principal.id
            // and return a UserResponse DTO.
            call.respond(HttpStatusCode.OK, principal)
        }
    }
}