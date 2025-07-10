package com.idaas.jwt

import com.idaas.user.domain.User
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.util.Date
import javax.crypto.SecretKey

class JwtService {

    // IMPORTANT: In a production environment, this key MUST be stored securely and not hardcoded.
    // It should be loaded from configuration.
    private val secretKey: SecretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256) // Generates a secure key
    private val expirationTimeMillis: Long = 3600000 // 1 hour

    fun generateToken(user: User): String {
        val now = Date()
        val expiration = Date(now.time + expirationTimeMillis)

        return Jwts.builder()
            .setSubject(user.id) // Using user ID as the subject
            .claim("email", user.email)
            .claim("name", user.name)
            .claim("roles", user.roles) // Add roles to JWT claims
            .setIssuedAt(now)
            .setExpiration(expiration)
            .signWith(secretKey)
            .compact()
    }

    // Basic validation logic, can be expanded or handled by Ktor auth plugin
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token)
            true
        } catch (e: Exception) {
            // Log error or handle specific exceptions like ExpiredJwtException, SignatureException
            println("Token validation failed: ${e.message}")
            false
        }
    }

    fun getUserIdFromToken(token: String): String? {
        return try {
            val claims = Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).body
            claims.subject
        } catch (e: Exception) {
            println("Failed to extract user ID from token: ${e.message}")
            null
        }
    }
}
