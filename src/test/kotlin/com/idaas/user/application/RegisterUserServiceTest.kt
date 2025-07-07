package com.idaas.user.application

import com.idaas.user.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegisterUserServiceTest {
    @Test
    fun `should register a new user if email is not taken`() {
        val repo = InMemoryUserRepository()
        val service = RegisterUserService(repo)
        val user = service.register("user@example.com", "Test User", "+1234567890")
        assertEquals("user@example.com", user.email)
        assertEquals("Test User", user.name)
        assertEquals("+1234567890", user.phone)
        assertEquals(user, repo.findByEmail("user@example.com"))
    }

    @Test
    fun `should fail if email is already registered`() {
        val repo = InMemoryUserRepository()
        val service = RegisterUserService(repo)
        service.register("user@example.com", "Test User", "+1234567890")
        assertFailsWith<IllegalArgumentException> {
            service.register("user@example.com", "Another User", "+1234567890")
        }
    }
} 