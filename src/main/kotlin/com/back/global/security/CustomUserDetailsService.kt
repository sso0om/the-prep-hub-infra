package com.back.global.security

import com.back.domain.member.member.service.MemberService
import com.back.global.exception.ServiceException
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val memberService: MemberService
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        return try {
            val member = memberService.findMemberByEmail(email)
            val memberId = member.id ?: throw ServiceException(400, "회원 id가 없습니다.")
            val memberTag = member.tag ?: throw ServiceException(400, "회원 tag가 없습니다.")
            val memberPassword = member.password ?: throw ServiceException(400, "비밀번호가 없습니다..")
            SecurityUser(
                memberId,
                member.nickname,
                memberTag,
                member.memberType,
                memberPassword,
                member.getAuthorities()
            )
        } catch (e: ServiceException) {
            throw UsernameNotFoundException("사용자를 찾을 수 없습니다: $email", e)
        }
    }
}
