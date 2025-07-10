package com.idaas.user.application

import com.idaas.user.domain.User
import com.idaas.user.domain.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mindrot.jbcrypt.BCrypt

class LoginUserServiceTest {

    private val userRepository: UserRepository = mockk()
    private val loginUserService = LoginUserService(userRepository)

    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val hashedPassword = BCrypt.hashpw(testPassword, BCrypt.gensalt())
    private val registeredUser = User(
        id = "user-123",
        email = testEmail,
        name = "Test User",
        phone = "+1234567890",
        hashedPassword = hashedPassword,
        roles = listOf("USER")
    )

    @Test
    fun `login should succeed with correct email and password`() {
        every { userRepository.findByEmail(testEmail) } returns registeredUser

        val resultUser = loginUserService.login(testEmail, testPassword)

        assertNotNull(resultUser)
        assertEquals(registeredUser.id, resultUser.id)
        assertEquals(registeredUser.email, resultUser.email)
        // The service returns the full user object, including hashed password.
        // This might be refined later to return a DTO.
        assertEquals(registeredUser.hashedPassword, resultUser.hashedPassword)
    }

    @Test
    fun `login should fail with incorrect password`() {
        every { userRepository.findByEmail(testEmail) } returns registeredUser

        val exception = assertThrows<AuthenticationException> {
            loginUserService.login(testEmail, "wrongPassword")
        }
        assertEquals("Invalid email or password.", exception.message)
    }

    @Test
    fun `login should fail with non-existent email`() {
        val nonExistentEmail = "nouser@example.com"
        every { userRepository.findByEmail(nonExistentEmail) } returns null

        val exception = assertThrows<AuthenticationException> {
            loginUserService.login(nonExistentEmail, "anyPassword")
        }
        assertEquals("Invalid email or password.", exception.message)
    }

    @Test
    fun `login should fail if user has no hashed password stored`() {
        val userWithNoPassword = User(
            id = "user-456",
            email = "nopass@example.com",
            name = "No Pass User",
            phone = "+0987654321",
            hashedPassword = null, // Explicitly null
            roles = listOf("USER")
        )
        every { userRepository.findByEmail("nopass@example.com") } returns userWithNoPassword

        val exception = assertThrows<AuthenticationException> {
            loginUserService.login("nopass@example.com", "anyPassword")
        }
        assertEquals("User account is not properly configured for password authentication.", exception.message)
    }
}
