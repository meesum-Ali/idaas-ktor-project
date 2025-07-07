package com.idaas.user.api

import com.idaas.user.application.RegisterUserService
import com.idaas.user.application.UpdateUserProfileService
import com.idaas.user.application.DeleteUserService
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
data class UserRequest(val email: String, val name: String, val phone: String)

fun Route.userApi(
    registerUserService: RegisterUserService,
    updateUserProfileService: UpdateUserProfileService,
    deleteUserService: DeleteUserService
) {
    post("/users") {
        val req = call.receive<UserRequest>()
        val user = registerUserService.register(req.email, req.name, req.phone)
        call.respond(HttpStatusCode.Created, user)
    }
    put("/users/{id}") {
        val id = call.parameters["id"]!!
        val req = call.receive<UserRequest>()
        try {
            val updated = updateUserProfileService.updateProfile(req.email, req.name, req.phone)
            call.respond(HttpStatusCode.OK, updated)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }
    delete("/users/{id}") {
        val id = call.parameters["id"]!!
        val req = call.receiveOrNull<UserRequest>()
        try {
            deleteUserService.deleteByEmail(req?.email ?: "")
            call.respond(HttpStatusCode.NoContent)
        } catch (e: IllegalArgumentException) {
            call.respond(HttpStatusCode.NotFound)
        }
    }
} 