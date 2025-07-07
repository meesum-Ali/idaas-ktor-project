package com.idaas.user.application

import com.idaas.user.domain.*
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DeleteUserServiceTest {
    @Test
    fun `should delete user if exists`() {
        val repo = InMemoryUserRepository()
        val user = User(email = "user@example.com", name = "Test User", phone = "+1234567890")
        repo.save(user)
        val service = DeleteUserService(repo)
        service.deleteByEmail("user@example.com")
        assertNull(repo.findByEmail("user@example.com"))
    }

    @Test
    fun `should throw if user does not exist`() {
        val repo = InMemoryUserRepository()
        val service = DeleteUserService(repo)
        assertFailsWith<IllegalArgumentException> {
            service.deleteByEmail("notfound@example.com")
        }
    }
} 