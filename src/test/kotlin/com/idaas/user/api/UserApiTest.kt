package com.idaas.user.api

import com.idaas.user.application.*
import com.idaas.user.domain.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import io.mockk.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class UserRequest(val email: String, val name: String, val phone: String)

fun Application.testUserApi(
    registerUserService: RegisterUserService,
    updateUserProfileService: UpdateUserProfileService,
    deleteUserService: DeleteUserService
) {
    routing {
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
}

class UserApiTest {
    @Test
    fun `should register user via API (BDD)`() = testApplication {
        environment {
            config = io.ktor.server.config.MapApplicationConfig()
        }
        // Given
        val registerUserService = mockk<RegisterUserService>()
        val user = User(id = "id-1", email = "apiuser@example.com", name = "API User", phone = "+1234567890")
        every { registerUserService.register(user.email, user.name, user.phone) } returns user
        // When
        application {
            install(ContentNegotiation) { json() }
            testUserApi(
                registerUserService = registerUserService,
                updateUserProfileService = mockk(relaxed = true),
                deleteUserService = mockk(relaxed = true)
            )
        }
        val json = Json { ignoreUnknownKeys = true }
        val registerReq = UserRequest(user.email, user.name, user.phone)
        val response = client.post("/users") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(registerReq))
        }
        // Then
        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = json.decodeFromString<User>(response.bodyAsText())
        assertEquals(user, responseBody)
        verify { registerUserService.register(user.email, user.name, user.phone) }
    }

    @Test
    fun `should update user via API (BDD)`() = testApplication {
        environment {
            config = io.ktor.server.config.MapApplicationConfig()
        }
        // Given
        val updateUserProfileService = mockk<UpdateUserProfileService>()
        val user = User(id = "id-1", email = "apiuser@example.com", name = "API User", phone = "+1234567890")
        val updatedUser = user.copy(name = "Updated User", phone = "+1987654321")
        every { updateUserProfileService.updateProfile(user.email, updatedUser.name, updatedUser.phone) } returns updatedUser
        // When
        application {
            install(ContentNegotiation) { json() }
            testUserApi(
                registerUserService = mockk(relaxed = true),
                updateUserProfileService = updateUserProfileService,
                deleteUserService = mockk(relaxed = true)
            )
        }
        val json = Json { ignoreUnknownKeys = true }
        val updateReq = UserRequest(user.email, updatedUser.name, updatedUser.phone)
        val response = client.put("/users/${'$'}{user.id}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateReq))
        }
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = json.decodeFromString<User>(response.bodyAsText())
        assertEquals(updatedUser, responseBody)
        verify { updateUserProfileService.updateProfile(user.email, updatedUser.name, updatedUser.phone) }
    }

    @Test
    fun `should delete user via API (BDD)`() = testApplication {
        environment {
            config = io.ktor.server.config.MapApplicationConfig()
        }
        // Given
        val deleteUserService = mockk<DeleteUserService>()
        every { deleteUserService.deleteByEmail("apiuser@example.com") } just Runs
        // When
        application {
            install(ContentNegotiation) { json() }
            testUserApi(
                registerUserService = mockk(relaxed = true),
                updateUserProfileService = mockk(relaxed = true),
                deleteUserService = deleteUserService
            )
        }
        val json = Json { ignoreUnknownKeys = true }
        val deleteReq = UserRequest("apiuser@example.com", "API User", "+1234567890")
        val response = client.delete("/users/id-1") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(deleteReq))
        }
        // Then
        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { deleteUserService.deleteByEmail("apiuser@example.com") }
    }

    @Test
    fun `should return 404 when deleting non-existent user via API (BDD)`() = testApplication {
        environment {
            config = io.ktor.server.config.MapApplicationConfig()
        }
        // Given
        val deleteUserService = mockk<DeleteUserService>()
        every { deleteUserService.deleteByEmail("notfound@example.com") } throws IllegalArgumentException("User not found")
        // When
        application {
            install(ContentNegotiation) { json() }
            testUserApi(
                registerUserService = mockk(relaxed = true),
                updateUserProfileService = mockk(relaxed = true),
                deleteUserService = deleteUserService
            )
        }
        val json = Json { ignoreUnknownKeys = true }
        val deleteReq = UserRequest("notfound@example.com", "Missing User", "+1234567890")
        val response = client.delete("/users/id-404") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(deleteReq))
        }
        // Then
        assertEquals(HttpStatusCode.NotFound, response.status)
        verify { deleteUserService.deleteByEmail("notfound@example.com") }
    }
} 