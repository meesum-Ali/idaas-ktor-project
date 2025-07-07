package com.idaas.user.domain

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserTest {
    @Test
    fun `should not allow creation with blank email`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "", name = "Test User", phone = "+1234567890")
        }
    }

    @Test
    fun `should not allow creation with blank name`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "user@example.com", name = "", phone = "+1234567890")
        }
    }

    @Test
    fun `should not allow creation with invalid email format`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "not-an-email", name = "Test User", phone = "+1234567890")
        }
    }

    @Test
    fun `should not allow creation with blank phone`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "user@example.com", name = "Test User", phone = "")
        }
    }

    @Test
    fun `should not allow creation with invalid phone format`() {
        assertFailsWith<IllegalArgumentException> {
            User(email = "user@example.com", name = "Test User", phone = "12345abc")
        }
    }

    @Test
    fun `should allow creation with valid email, name, and phone`() {
        val user = User(email = "user@example.com", name = "Test User", phone = "+1234567890")
        assertTrue(user.id.isNotBlank())
        assertEquals("user@example.com", user.email)
        assertEquals("Test User", user.name)
        assertEquals("+1234567890", user.phone)
    }
} 