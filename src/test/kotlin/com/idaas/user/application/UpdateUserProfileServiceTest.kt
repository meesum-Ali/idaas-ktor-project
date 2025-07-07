package com.idaas.user.application

import com.idaas.user.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateUserProfileServiceTest {
    @Test
    fun `should update user name and phone if user exists`() {
        val repo = InMemoryUserRepository()
        val user = User(email = "user@example.com", name = "Old Name", phone = "+1234567890")
        repo.save(user)
        val service = UpdateUserProfileService(repo)
        val updated = service.updateProfile("user@example.com", "New Name", "+1987654321")
        assertEquals("New Name", updated.name)
        assertEquals("+1987654321", updated.phone)
        assertEquals(updated, repo.findByEmail("user@example.com"))
    }

    @Test
    fun `should throw if user does not exist`() {
        val repo = InMemoryUserRepository()
        val service = UpdateUserProfileService(repo)
        assertFailsWith<IllegalArgumentException> {
            service.updateProfile("notfound@example.com", "New Name", "+1987654321")
        }
    }
} 