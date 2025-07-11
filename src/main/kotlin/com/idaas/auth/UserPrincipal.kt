package com.idaas.auth

import io.ktor.server.auth.*

data class UserPrincipal(
    val id: String,
    val email: String, // Keep email for potential logging or other uses
    val name: String,
    val roles: List<String>
) : Principal
