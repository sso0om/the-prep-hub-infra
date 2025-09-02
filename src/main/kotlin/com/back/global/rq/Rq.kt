package com.back.global.rq

import com.back.domain.member.member.entity.Member
import com.back.global.security.SecurityUser
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@RequestScope
@Component
class Rq (
    private val req: HttpServletRequest,
    private val resp: HttpServletResponse
) {
    val actor: Member?
        // Spring Security를 사용하여 인증된 사용자의 정보를 가져오는 메소드
        get() {
            val principal = SecurityContextHolder.getContext().authentication?.principal

            return (principal as? SecurityUser)?.let { securityUser ->
                Member(
                    securityUser.id,
                    securityUser.nickname,
                    securityUser.memberType,
                    securityUser.tag,
                )
            }
        }

    fun getHeader(name: String, defaultValue: String): String =
        req.getHeader(name)
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue

    fun setHeader(name: String, value: String?) {
        resp.setHeader(name, value ?: "")
    }

    fun getCookieValue(name: String, defaultValue: String): String =
        req.cookies
            ?.find { it.name == name }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: defaultValue

    fun setCookie(name: String, value: String?) {
        val cookieValue = value ?: ""
        val maxAgeSeconds = 60 * 60 * 24 * 365

        Cookie(name, value ?: "").apply {
            path = "/"
            isHttpOnly = true
            domain = "localhost"
            secure = true
            setAttribute("SameSite", "Strict")
            maxAge = if (cookieValue.isBlank()) 0 else maxAgeSeconds
        }.let { resp.addCookie(it)  }
    }

    fun deleteCookie(name: String) {
        setCookie(name, null)
    }
}
