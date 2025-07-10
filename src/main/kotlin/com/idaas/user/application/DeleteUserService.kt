package com.idaas.user.application

import com.idaas.user.domain.UserRepository

class DeleteUserService(private val userRepository: UserRepository) {
    fun deleteByEmail(email: String) { // Keep for now if used elsewhere, or mark as deprecated
        userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("User with email $email not found for deletion")
        userRepository.deleteByEmail(email)
    }

    fun deleteById(id: String) {
        userRepository.findById(id)
            ?: throw IllegalArgumentException("User with id $id not found for deletion")
        userRepository.deleteById(id)
    }
}