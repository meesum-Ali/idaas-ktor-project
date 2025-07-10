package com.idaas.user.application

import com.idaas.user.domain.User
import com.idaas.user.domain.UserRepository
import org.mindrot.jbcrypt.BCrypt

class RegisterUserService(private val userRepository: UserRepository) {
    fun register(email: String, name: String, phone: String, password: String): User {
        if (userRepository.findByEmail(email) != null) {
            throw IllegalArgumentException("Email is already registered")
        }
        require(password.isNotBlank()) { "Password must not be blank" }
        // Add more password validation rules if needed (e.g., length, complexity)

        val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
        val user = User(email = email, name = name, phone = phone, hashedPassword = hashedPassword)
        userRepository.save(user)
        // It's good practice to not return the hashed password, even in the registration response.
        // However, the current User model includes it. For now, we'll return the full user object.
        // Consider creating a UserResponse DTO later if sensitive fields need to be excluded.
        return user
    }
}