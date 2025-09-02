package com.back.domain.member.member.support

import com.back.domain.api.service.ApiKeyService
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.entity.MemberInfo
import com.back.domain.member.member.repository.MemberInfoRepository
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.member.member.service.MemberService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.util.*

@Component
class MemberFixture(
    private val memberRepository: MemberRepository,
    private val memberInfoRepository: MemberInfoRepository,
    private val memberService: MemberService
) {

    private val apiKeyService = ApiKeyService()
    private val passwordEncoder = BCryptPasswordEncoder()

    fun createMember(i: Int): Member {
        // 1. 태그 생성 (중복 확인)
        val tag = generateUniqueTag(i)

        // 2. 회원 엔티티 생성 (비밀번호는 해시처리)
        val member = Member.createMember(
            nickname = "테스트유저$i",
            password = passwordEncoder.encode("password123"),
            tag = tag
        )

        // 3. 회원정보 엔티티 생성 + 연관관계 설정
        val memberInfo = MemberInfo().apply {
            email = "test$i@example.com"
            bio = "소개입니다"
            profileImageUrl = null
            apiKey = apiKeyService.generateApiKey()
            this.setMember(member)
        }

        member.setMemberInfo(memberInfo)

        // 4. 저장 (Member 먼저 저장해야 ID가 생성됨)
        memberRepository.save(member)
        memberInfoRepository.save(memberInfo)

        return member
    }

    fun createMultipleMember(count: Int): List<Member> =
        (1..count).map { createMember(it) }

    private fun generateUniqueTag(i: Int): String {
        var tag: String
        do {
            tag = UUID.randomUUID().toString().substring(0, 6)
        } while (memberRepository.existsByNicknameAndTag("테스트유저$i", tag))
        return tag
    }
}
