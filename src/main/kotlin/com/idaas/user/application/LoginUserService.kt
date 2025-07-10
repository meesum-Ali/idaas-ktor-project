package com.idaas.user.application

import com.idaas.user.domain.User
import com.idaas.user.domain.UserRepository
import org.mindrot.jbcrypt.BCrypt

class LoginUserService(private val userRepository: UserRepository) {

    fun login(email: String, password: String): User {
        val user = userRepository.findByEmail(email)
            ?: throw AuthenticationException("Invalid email or password.")

        if (user.hashedPassword == null) {
            // This case should ideally not happen for a registered user if registration enforces password.
            // Or, it could mean the user was created before password field was mandatory.
            throw AuthenticationException("User account is not properly configured for password authentication.")
        }

        if (!BCrypt.checkpw(password, user.hashedPassword)) {
            throw AuthenticationException("Invalid email or password.")
        }

        // Successfully authenticated
        // Consider returning a DTO that doesn't include hashedPassword
        return user
    }
}

class AuthenticationException(message: String) : RuntimeException(message)
