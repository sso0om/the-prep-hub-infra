package com.back.global.security

import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import com.back.standard.util.Ut
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class CustomAuthenticationFilter(
    private val memberService: MemberService,
    private val rq: Rq
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        logger.debug("Processing request for ${request.requestURI}")

        try {
            work(request, response, filterChain)
        } catch (e: ServiceException) {
            val rsData: RsData<*> = e.getRsData()
            response.contentType = "application/json;charset=UTF-8"
            response.status = rsData.code
            response.writer.write(Ut.json.toString(rsData))
            return
        } catch (e: Exception) {
            logger.error("Unhandled exception in CustomAuthenticationFilter", e)
            response.contentType = "application/json;charset=UTF-8"
            response.status = 500
            response.writer.write("""{"code":500,"msg":"서버 오류가 발생했습니다."}""")
            return
        }
    }

    private fun work(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        // API 요청이 아니라면 패스
        if (!request.requestURI.startsWith("/api/")) {
            filterChain.doFilter(request, response)
            return
        }

        // 인증, 인가가 필요없는 API 요청이라면 패스
        val openApis = listOf(
            "/api/v1/members/auth/login",
            "/api/v1/members/auth/register",
            "/api/v1/members/auth/guest-register",
            "/api/v1/members/auth/guest-login",
            "/api/v1/clubs/public"
        )
        if (request.requestURI in openApis || request.requestURI.startsWith("/api/v1/clubs/public/")) {
            filterChain.doFilter(request, response)
            return
        }

        val accessToken: String = rq.getHeader("Authorization", "").trim()
            .takeIf { it.isNotBlank() }?.let {
                if (!it.startsWith("Bearer ", ignoreCase = true) || it.length <= 7)
                    throw ServiceException(401, "Authorization 헤더가 Bearer 형식이 아닙니다.")
                it.substring(7).trim()
            } ?: rq.getCookieValue("accessToken", "").trim()

        if (accessToken.isBlank()) {
            filterChain.doFilter(request, response)
            return
        }

        var member: Member? = null
        var isAccessTokenValid = false

        // accessToken이 존재하는 경우, 해당 토큰의 유효성을 검사
        if (accessToken.isNotBlank()) {
            try {
                val payload = memberService.payload(accessToken)
                val email = payload?.get("email") as? String
                if (!email.isNullOrBlank()) {
                    val dbMember = memberService.findMemberByEmail(email)
                    if (dbMember != null) {
                        member = dbMember
                        isAccessTokenValid = true
                    }
                }
            } catch (ex: Exception) {
                isAccessTokenValid = false
            }
        }


        if (!isAccessTokenValid) {
            throw ServiceException(499, "access token이 유효하지 않습니다.")
        }

        val m = member ?: throw ServiceException(401, "Access Token이 유효하지 않습니다.")
        val user = SecurityUser(
            m.id ?: throw ServiceException(400, "회원 id가 없습니다."),
            m.nickname ?: throw ServiceException(400, "회원 nickname이 없습니다."),
            m.tag ?: throw ServiceException(400, "회원 tag가 없습니다."),
            m.memberType,
            m.password ?: throw ServiceException(400, "비밀번호가 없습니다."),
            m.getAuthorities()
        )


        val auth = UsernamePasswordAuthenticationToken(
            user,
            null,
            user.authorities
        )
        auth.details = org.springframework.security.web.authentication.WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = auth


        filterChain.doFilter(request, response)
    }
}
