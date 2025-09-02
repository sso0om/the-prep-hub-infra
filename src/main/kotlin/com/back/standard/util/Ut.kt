package com.back.standard.util

import com.back.global.exception.ServiceException
import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ClaimsBuilder
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.security.Key
import java.util.*

object Ut {

    object jwt {

        fun generate(secret: String, expireSeconds: Int, body: Map<String, Any>): String {
            val claimsBuilder: ClaimsBuilder = Jwts.claims()

            for ((key, value) in body) {
                claimsBuilder.add(key, value)
            }

            val claims: Claims = claimsBuilder.build()
            val issuedAt = Date()
            val expiration = Date(issuedAt.time + 1000L * expireSeconds)

            val secretKey: Key = Keys.hmacShaKeyFor(secret.toByteArray())

            return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(secretKey)
                .compact()
        }

        fun payload(secret: String, jwtStr: String): Map<String, Any>? {
            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

            return try {
                @Suppress("UNCHECKED_CAST")
                Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwtStr)
                    .payload as Map<String, Any>
            } catch (e: Exception) {
                null
            }
        }
    }

    object json {
        var objectMapper: ObjectMapper? = null

        fun toString(obj: Any, defaultValue: String? = null): String? {
            return try {
                objectMapper?.writeValueAsString(obj)
            } catch (e: Exception) {
                defaultValue
            }
        }
    }
}

fun <T> T?.orServiceThrow(message: String): T =
    this ?: throw ServiceException(400, message)
