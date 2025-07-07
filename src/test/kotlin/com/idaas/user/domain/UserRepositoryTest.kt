package com.idaas.user.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserRepositoryTest {
    @Test
    fun `should save and retrieve user by email`() {
        val repo = InMemoryUserRepository()
        val user = User(email = "user@example.com", name = "Test User", phone = "+1234567890")
        repo.save(user)
        val found = repo.findByEmail("user@example.com")
        assertEquals(user, found)
    }

    @Test
    fun `should return null for non-existent user`() {
        val repo = InMemoryUserRepository()
        val found = repo.findByEmail("notfound@example.com")
        assertNull(found)
    }

    @Test
    fun `should update user if saved with same email`() {
        val repo = InMemoryUserRepository()
        val user1 = User(email = "user@example.com", name = "User One", phone = "+1234567890")
        val user2 = User(email = "user@example.com", name = "User Two", phone = "+1987654321")
        repo.save(user1)
        repo.save(user2)
        val found = repo.findByEmail("user@example.com")
        assertEquals(user2, found)
    }

    @Test
    fun `should delete user by email`() {
        val repo = InMemoryUserRepository()
        val user = User(email = "user@example.com", name = "Test User", phone = "+1234567890")
        repo.save(user)
        repo.deleteByEmail("user@example.com")
        val found = repo.findByEmail("user@example.com")
        assertNull(found)
    }

    @Test
    fun `should list all users`() {
        val repo = InMemoryUserRepository()
        val user1 = User(email = "user1@example.com", name = "User One", phone = "+1234567890")
        val user2 = User(email = "user2@example.com", name = "User Two", phone = "+1987654321")
        repo.save(user1)
        repo.save(user2)
        val all = repo.findAll().toSet()
        assertEquals(setOf(user1, user2), all)
    }
} 