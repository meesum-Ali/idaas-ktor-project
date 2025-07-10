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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.idaas.jwt.JwtService // Import real JwtService for mocking if needed, or use its interface

// Use the actual request/response data classes from the main source
// No need to redefine UserRequest if it's identical to UserRegistrationRequest or another existing one.
// Let's assume we'll use the main source data classes:
// com.idaas.user.api.UserRegistrationRequest
// com.idaas.user.api.UserLoginRequest
// com.idaas.user.api.UserUpdateRequest

@Serializable // Helper for JWT response
data class TokenResponse(val token: String)


class UserApiTest {

    // Mock services that will be injected into the API
    private val registerUserService: RegisterUserService = mockk()
    private val updateUserProfileService: UpdateUserProfileService = mockk(relaxUnitFun = true) // relaxUnitFun for void returns
    private val deleteUserService: DeleteUserService = mockk(relaxUnitFun = true)
    private val loginUserService: LoginUserService = mockk()
    private val jwtService: JwtService = mockk() // Mock the actual JwtService

    // Define a helper to setup the application for tests
    private fun Application.setupTestApi() {
        install(ContentNegotiation) { json() } // Use kotlinx.serialization.json
        routing {
            // Use the actual userApi from main source
            userApi(
                registerUserService,
                updateUserProfileService,
                deleteUserService,
                loginUserService,
                jwtService
            )
        }
    }


    @Test
    fun `POST users_register should register user and return user object`() = testApplication {
        application { setupTestApi() } // Configure the test application

        val request = UserRegistrationRequest("test@example.com", "Test User", "+1234567890", "password123")
        val expectedUser = User(
            id = "generated-id",
            email = request.email,
            name = request.name,
            phone = request.phone,
            hashedPassword = "hashed-password-from-service", // Service will hash it
            roles = listOf("USER")
        )

        every { registerUserService.register(request.email, request.name, request.phone, request.password) } returns expectedUser

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/users/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = Json.decodeFromString<User>(response.bodyAsText())
        assertEquals(expectedUser.email, responseBody.email)
        assertEquals(expectedUser.name, responseBody.name)
        // Note: The actual User object returned might have more fields (like ID, hashedPassword)
        // We should assert based on what's expected to be returned by the API (DTO or full User)
        // For now, UserApi returns the full User object.
        assertNotNull(responseBody.id)
        assertNotNull(responseBody.hashedPassword) // API currently returns this
        assertEquals(expectedUser.roles, responseBody.roles)


        verify { registerUserService.register(request.email, request.name, request.phone, request.password) }
    }

    @Test
    fun `POST users_login should return JWT for valid credentials`() = testApplication {
        application { setupTestApi() }

        val loginRequest = UserLoginRequest("test@example.com", "password123")
        val userFromDb = User(
            id = "user-123",
            email = loginRequest.email,
            name = "Test User",
            phone = "+1234567890",
            hashedPassword = "hashed-actual-password", // Assume this is valid
            roles = listOf("USER")
        )
        val expectedToken = "mocked.jwt.token"

        every { loginUserService.login(loginRequest.email, loginRequest.password) } returns userFromDb
        every { jwtService.generateToken(userFromDb) } returns expectedToken

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/users/login") {
            contentType(ContentType.Application.Json)
            setBody(loginRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val tokenResponse = Json.decodeFromString<TokenResponse>(response.bodyAsText())
        assertEquals(expectedToken, tokenResponse.token)

        verify { loginUserService.login(loginRequest.email, loginRequest.password) }
        verify { jwtService.generateToken(userFromDb) }
    }

    @Test
    fun `POST users_login should return 401 for invalid credentials`() = testApplication {
        application { setupTestApi() }

        val loginRequest = UserLoginRequest("test@example.com", "wrongpassword")

        every { loginUserService.login(loginRequest.email, loginRequest.password) } throws AuthenticationException("Invalid email or password.")

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/users/login") {
            contentType(ContentType.Application.Json)
            setBody(loginRequest)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        verify { loginUserService.login(loginRequest.email, loginRequest.password) }
        verify(exactly = 0) { jwtService.generateToken(any()) }
    }


    // TODO: Add/Update tests for PUT /users/{id} and DELETE /users/{id}
    // These tests need to be updated to:
    // 1. Use the correct path parameter {id} for identifying the user. (Done in previous step)
    // 2. Reflect changes in service layer methods if any (e.g., using ID instead of email for lookup). (Done)
    // 3. Use UserUpdateRequest for PUT. (Done)
    // 4. Potentially incorporate JWT authentication if these endpoints become protected. (Out of scope for current plan step)

    @Test
    fun `PUT users_{id} should update user successfully`() = testApplication {
        application { setupTestApi() }

        val userId = "user-to-update-id"
        val updateRequest = UserUpdateRequest(name = "Updated Name", phone = "+1112223333")
        val updatedUserFromService = User(
            id = userId,
            email = "original@example.com", // Email is not changed by this request
            name = updateRequest.name,
            phone = updateRequest.phone,
            hashedPassword = "somehash",
            roles = listOf("USER")
        )

        every { updateUserProfileService.updateProfile(userId, updateRequest.name, updateRequest.phone) } returns updatedUserFromService

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/users/$userId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.decodeFromString<User>(response.bodyAsText())
        assertEquals(updatedUserFromService.name, responseBody.name)
        assertEquals(updatedUserFromService.phone, responseBody.phone)
        assertEquals(userId, responseBody.id)

        verify { updateUserProfileService.updateProfile(userId, updateRequest.name, updateRequest.phone) }
    }

    @Test
    fun `PUT users_{id} should return 404 if user not found`() = testApplication {
        application { setupTestApi() }

        val userId = "non-existent-id"
        val updateRequest = UserUpdateRequest(name = "Updated Name", phone = "+1112223333")

        every { updateUserProfileService.updateProfile(userId, updateRequest.name, updateRequest.phone) } throws IllegalArgumentException("User with id $userId not found")

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/users/$userId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify { updateUserProfileService.updateProfile(userId, updateRequest.name, updateRequest.phone) }
    }

    @Test
    fun `DELETE users_{id} should delete user successfully`() = testApplication {
        application { setupTestApi() }

        val userId = "user-to-delete-id"
        every { deleteUserService.deleteById(userId) } just runs // Using 'just runs' for void methods

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.delete("/users/$userId")

        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { deleteUserService.deleteById(userId) }
    }

    @Test
    fun `DELETE users_{id} should return 404 if user not found`() = testApplication {
        application { setupTestApi() }

        val userId = "non-existent-id-for-delete"
        every { deleteUserService.deleteById(userId) } throws IllegalArgumentException("User with id $userId not found for deletion")

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.delete("/users/$userId")

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify { deleteUserService.deleteById(userId) }
    }
}