package com.idaas.user.application

import com.idaas.user.domain.User
import com.idaas.user.domain.UserRepository

class UpdateUserProfileService(private val userRepository: UserRepository) {
    fun updateProfile(email: String, name: String, phone: String): User {
        val existing = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found")
        val updated = User(
            id = existing.id,
            email = existing.email,
            name = name,
            phone = phone
        )
        userRepository.save(updated)
        return updated
    }
} 