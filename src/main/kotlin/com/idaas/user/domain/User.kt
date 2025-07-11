package com.idaas.user.domain

import java.util.UUID
import kotlinx.serialization.Serializable

private val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
private val PHONE_REGEX = "^\\+?[1-9]\\d{1,14}$".toRegex() // E.164 format

@Serializable
data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val name: String,
    val phone: String,
    val hashedPassword: String? = null, // Nullable for now, will be set during registration
    val roles: List<String> = listOf("USER") // Default role
) {
    init {
        require(id.isNotBlank()) { "Id must not be blank" }
        require(email.isNotBlank()) { "Email must not be blank" }
        require(name.isNotBlank()) { "Name must not be blank" }
        require(email.matches(EMAIL_REGEX)) { "Email must be a valid email address" }
        require(phone.isNotBlank()) { "Phone must not be blank" }
        require(phone.matches(PHONE_REGEX)) { "Phone must be a valid E.164 number (e.g. +1234567890)" }
    }
} 