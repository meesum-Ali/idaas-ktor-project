package com.idaas.user.application

import com.idaas.user.domain.UserRepository

class DeleteUserService(private val userRepository: UserRepository) {
    fun deleteByEmail(email: String) {
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User not found")
        userRepository.deleteByEmail(email)
    }
} 