package com.back.global.security

import com.back.global.enums.MemberType
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

@Component
@Profile("test") // 테스트 프로파일에서만 활성화
@Order(1)
class MockAuthFilterForSpecificApi : OncePerRequestFilter() {

    @Throws(ServletException::class)
    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        // 특정 API 경로에만 필터 적용
        return !request.requestURI.startsWith("/api/v1/schedules")
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val context = SecurityContextHolder.getContext()

        // 이미 인증 정보가 있으면 패스
        if (context.authentication == null) {
            val testUser = SecurityUser(
                id = 1L,
                nickname = "홍길동",
                tag = "fakeTag",
                memberType = MemberType.MEMBER,
                password = "password1",
                authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
            )

            val auth = UsernamePasswordAuthenticationToken(
                testUser,
                null,
                testUser.authorities
            )

            context.authentication = auth
        }

        filterChain.doFilter(request, response)
    }
}
