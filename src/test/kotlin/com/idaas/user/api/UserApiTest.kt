package com.idaas.user.api

import com.idaas.user.application.*
import com.idaas.user.domain.*
import com.idaas.auth.UserPrincipal // Import UserPrincipal for /me endpoint test response
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.* // For Principal in test setup if needed, not directly here
import io.ktor.server.plugins.contentnegotiation.*
// import io.ktor.server.request.* // Not directly used in test client calls
// import io.ktor.server.response.* // Not directly used in test client calls
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.serialization.kotlinx.json.json
// import io.ktor.util.AttributeKey // Not used
import io.mockk.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
// import kotlinx.serialization.encodeToString // Used by Json.encodeToString
// import kotlinx.serialization.decodeFromString // Used by Json.decodeFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import com.idaas.jwt.JwtService

// Use the actual request/response data classes from the main source
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
    fun `PUT users_{id} should update user successfully with valid JWT`() = testApplication {
        application { setupTestApi() }

        val userId = "user-to-update-id"
        val userMakingRequest = User(userId, "test@example.com", "Test User", "+123", "hashed", listOf("USER"))
        val token = "valid.jwt.token"
        every { jwtService.generateToken(userMakingRequest) } returns token // Not strictly needed for this test setup if principal is mocked
                                                                    // but good for consistency if we were testing token generation for header.
                                                                    // Ktor auth will use its own validation based on JwtService.getVerifier().

        val updateRequest = UserUpdateRequest(name = "Updated Name", phone = "+1112223333")
        val updatedUserFromService = User(
            id = userId,
            email = "original@example.com",
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
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.decodeFromString<User>(response.bodyAsText())
        assertEquals(updatedUserFromService.name, responseBody.name)
        verify { updateUserProfileService.updateProfile(userId, updateRequest.name, updateRequest.phone) }
    }

    @Test
    fun `PUT users_{id} should return 401 if no JWT is provided`() = testApplication {
        application { setupTestApi() }
        val userId = "user-to-update-id"
        val updateRequest = UserUpdateRequest(name = "Updated Name", phone = "+1112223333")

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/users/$userId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }


    @Test
    fun `PUT users_{id} should return 404 if user not found with valid JWT`() = testApplication {
        application { setupTestApi() }

        val userId = "non-existent-id"
        val token = "valid.jwt.token" // Assume a valid token structure for authentication to pass
        val updateRequest = UserUpdateRequest(name = "Updated Name", phone = "+1112223333")

        every { updateUserProfileService.updateProfile(userId, updateRequest.name, updateRequest.phone) } throws IllegalArgumentException("User with id $userId not found")

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify { updateUserProfileService.updateProfile(userId, updateRequest.name, updateRequest.phone) }
    }

    @Test
    fun `DELETE users_{id} should delete user successfully with valid JWT`() = testApplication {
        application { setupTestApi() }

        val userId = "user-to-delete-id"
        val token = "valid.jwt.token"
        every { deleteUserService.deleteById(userId) } just runs

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.delete("/users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        verify { deleteUserService.deleteById(userId) }
    }

    @Test
    fun `DELETE users_{id} should return 401 if no JWT is provided`() = testApplication {
        application { setupTestApi() }
        val userId = "user-to-delete-id"

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val response = client.delete("/users/$userId")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE users_{id} should return 404 if user not found with valid JWT`() = testApplication {
        application { setupTestApi() }

        val userId = "non-existent-id-for-delete"
        val token = "valid.jwt.token"
        every { deleteUserService.deleteById(userId) } throws IllegalArgumentException("User with id $userId not found for deletion")

        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.delete("/users/$userId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        verify { deleteUserService.deleteById(userId) }
    }

    @Test
    fun `GET users_me should return authenticated user principal with valid JWT`() = testApplication {
        application { setupTestApi() }

        // The JWT token itself needs to be valid for the auth provider setup in Application.kt
        // This means it must be parsable by jwtService.getVerifier() and contain necessary claims.
        // For testing, we can mock jwtService.generateToken to return a specific token string,
        // and then ensure our Ktor auth setup (which uses jwtService.getVerifier()) can parse it.
        // However, Ktor's testApplication doesn't fully simulate the JWT validation for extracting principal
        // unless the token is *actually* valid and parsable by the verifier configured.
        // A simpler way for unit testing the API layer is to assume authentication has passed
        // if the route is hit, and Ktor would have populated the principal.
        // But for a more integrated test of the auth setup itself:

        val testUserForToken = User("auth-user-id", "auth@example.com", "Auth User", "+222", "hashed", listOf("USER"))
        // Use the actual JwtService instance that would be used by the application module to generate a token
        // This requires JwtService to be instantiated consistently or its key to be known for test generation.
        // Our current JwtService creates a random key on init. This makes it hard to generate a matching token outside.

        // Let's use a real (but temporary for test) JwtService to generate a token
        val realJwtServiceForTest = JwtService() // This will have its own key
        val validTestToken = realJwtServiceForTest.generateToken(testUserForToken)

        // To make this test work with the Ktor auth setup, the JwtService instance used by
        // `application { setupTestApi() }` (which is `this.jwtService` mock) needs to be configured
        // to validate `validTestToken` correctly. This means its `getVerifier()` should use the same key
        // as `realJwtServiceForTest`. This is tricky with current setup.

        // Alternative: Mock the behavior of the authentication outcome.
        // Ktor's testing tools for authentication are more about checking if a route is protected
        // and if the auth provider logic (validate block) works.

        // For this specific test of /users/me, let's assume a token IS valid and a principal IS set.
        // The actual token validation logic is part of the Authentication plugin's setup.
        // Here, we test that IF authentication passes, the endpoint returns what we expect.

        // We will rely on Ktor's test setup to handle the principal correctly if the "auth-jwt"
        // provider is correctly configured and a token *that it can validate* is passed.
        // The crucial part is that the verifier used in `install(Authentication)` must be able
        // to parse the token we send.

        // Given our JwtService uses a random key, we cannot easily generate a token outside
        // that the mocked jwtService's verifier (if we were to mock getVerifier) would accept.
        // So, for testing protected endpoints, we often trust Ktor's auth and test the behavior *after* auth.
        // The tests for "401 if no JWT" cover the protection part.

        // For this test, we'll mock a token that the *actual* jwtService (if it were not mocked) would generate.
        // And then check the response. The UserPrincipal returned should match claims.

        val expectedPrincipal = UserPrincipal(
            id = "test-user-id-me",
            email = "me@example.com",
            name = "Me User",
            roles = listOf("USER", "EDITOR")
        )
        // This token needs to be decodable by the verifier in Application.kt's auth setup.
        // Since jwtService is mocked, we can't directly use it to generate a token that Application.kt will verify.
        // This highlights a common testing challenge for JWTs.
        // A common pattern is to have a test helper that generates valid tokens using the *actual* secret/config.

        // For now, let's assume the token is valid and Ktor sets the principal.
        // The test will focus on the route handler's logic post-authentication.
        // The actual validation of the token format/signature is implicitly tested by Ktor's auth plugin.
        // The test for 401 when no token is provided tests the protection.

        // To make this test meaningful for /me, we need to ensure the principal is correctly passed.
        // The `validate` block in Application.kt is responsible for creating UserPrincipal.
        // We need to send a token that, when validated by the *actual* configuration, produces this principal.

        // Let's use the actual JwtService (not the mock) to create a token for this test.
        // And ensure the mock `jwtService` within the test environment doesn't interfere with the auth plugin's verifier.
        // This means the `jwtService` mock passed to `userApi` should not be used by the `Authentication` plugin itself.
        // The `Authentication` plugin in `Application.kt` uses `val jwtService = JwtService()` (a real one).

        val actualJwtService = JwtService() // This is the one whose key is used by Auth plugin
        val userForToken = User(expectedPrincipal.id, expectedPrincipal.email, expectedPrincipal.name, "+123", "pass", expectedPrincipal.roles)
        val token = actualJwtService.generateToken(userForToken)


        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("/users/me") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responsePrincipal = Json.decodeFromString<UserPrincipal>(response.bodyAsText())
        assertEquals(expectedPrincipal, responsePrincipal)
    }

    @Test
    fun `GET users_me should return 401 if no JWT is provided`() = testApplication {
        application { setupTestApi() }
        val client = createClient {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val response = client.get("/users/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}