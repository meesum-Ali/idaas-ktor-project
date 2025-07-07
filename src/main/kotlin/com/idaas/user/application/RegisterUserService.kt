package com.idaas.user.application

import com.idaas.user.domain.User
import com.idaas.user.domain.UserRepository

class RegisterUserService(private val userRepository: UserRepository) {
    fun register(email: String, name: String, phone: String): User {
        if (userRepository.findByEmail(email) != null) {
            throw IllegalArgumentException("Email is already registered")
        }
        val user = User(email = email, name = name, phone = phone)
        userRepository.save(user)
        return user
    }
} 