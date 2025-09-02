package com.back.domain.auth.service

import com.back.domain.member.member.entity.Member
import com.back.global.config.jwt.JwtProperties
import com.back.standard.util.Ut
import org.springframework.stereotype.Service

@Service
class AuthService(private val jwtProperties: JwtProperties) {

    fun generateAccessToken(member: Member): String {
        // 1. 핵심 식별자 검증
        val id = member.id ?: throw IllegalStateException("회원 ID가 없습니다.")
        val nickname = member.nickname
        val tag = member.tag
        val memberType = member.memberType

        // 2. 회원 검증
        val email = member.getMemberInfo()?.email ?: ""

        val expiration = (jwtProperties.accessToken.expirationSeconds ?: 3600L)
                    .coerceAtLeast(60L) // 최소 60초
                    .coerceAtMost(Int.MAX_VALUE.toLong())
                    .toInt()

        return Ut.jwt.generate(
            jwtProperties.jwt.secretKey!!,
            expiration,
            mapOf(
                "id" to id,
                "email" to email,
                "nickname" to nickname,
                "tag" to (tag ?: ""),
                "memberType" to memberType.name
            )
        )
    }

    fun payload(accessToken: String): Map<String, Any>? {
        val parsedPayload = Ut.jwt.payload(jwtProperties.jwt.secretKey!!, accessToken) ?: return null
        val email = parsedPayload["email"] as? String ?: ""
        return mapOf("email" to email)
    }
}
