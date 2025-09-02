package com.back.global.config.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "custom")
class JwtProperties(
    val jwt: Jwt,
    val accessToken: AccessToken
) {
    fun getExpirationSecondsAsLong(): Long =
        accessToken.expirationSeconds
            ?: throw IllegalStateException("expirationSeconds is null")
}

    // Top-level class
    class Jwt(
        var secretKey: String? = null
    ) {
        override fun toString(): String = "Jwt(secretKey=**redacted**)"
    }

    // Top-level data class
    data class AccessToken(
        var expirationSeconds: Long? = null
    )

