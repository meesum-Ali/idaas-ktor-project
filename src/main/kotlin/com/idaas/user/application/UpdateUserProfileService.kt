package com.idaas.user.application

import com.idaas.user.domain.User
import com.idaas.user.domain.UserRepository

class UpdateUserProfileService(private val userRepository: UserRepository) {
    fun updateProfile(id: String, name: String, phone: String): User {
        val existing = userRepository.findById(id)
            ?: throw IllegalArgumentException("User with id $id not found")

        // Ensure email is not accidentally changed by `copy` if not intended
        // The UserUpdateRequest doesn't contain email, so existing.email is preserved.
        val updated = existing.copy(
            name = name,
            phone = phone
            // Password updates should be handled by a dedicated service.
            // Role updates would also likely be a separate, more privileged operation.
        )
        userRepository.save(updated) // save method in repo updates based on ID
        return updated
    }

    fun findUserById(id: String): User? { // This method is still useful for API layer checks or other services
        return userRepository.findById(id)
    }
}