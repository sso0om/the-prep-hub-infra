package com.back.domain.club.clubMember.dtos

import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.enums.MemberType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.util.*

/**
 * 클럽 멤버 정보를 등록하기 위한 DTO 클래스
 */
data class ClubMemberRegisterInfo(
    @field:NotBlank
    val email: String,
    @field:NotBlank
    val role: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val that = other as ClubMemberRegisterInfo
        return email == that.email // 이메일만 기준
    }

    override fun hashCode(): Int {
        return Objects.hash(email)
    }
}

data class ClubMemberRegisterRequest(
    @field:NotEmpty
    val members: List<ClubMemberRegisterInfo>
)

data class ClubMemberRoleChangeRequest(
    @field:NotBlank
    val role : String
)

data class ClubMemberInfo(
    val clubMemberId: Long,
    val memberId: Long,
    val nickname: String,
    val tag: String,
    val role: ClubMemberRole,
    val email: String,
    val memberType: MemberType,
    val profileImageUrl: String?,
    val state: ClubMemberState
)

data class ClubMemberResponse(
    val members: MutableList<ClubMemberInfo>
)
