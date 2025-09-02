package com.back.domain.member.friend.dto

import com.back.domain.member.member.entity.Member
import io.swagger.v3.oas.annotations.media.Schema

data class FriendMemberDto(
    @Schema(description = "친구(회원) ID")
    val friendMemberId: Long,
    @Schema(description = "친구(회원) 닉네임")
    val friendNickname: String,
    @Schema(description = "친구(회원) 자기소개")
    val friendBio: String?,
    @Schema(description = "친구(회원) 프로필 이미지 URL")
    val friendProfileImageUrl: String?
) {
    companion object {
        fun from(friendMember: Member): FriendMemberDto {
            val memberInfo = friendMember.getMemberInfo()

            return FriendMemberDto(
                friendMemberId = friendMember.id!!,
                friendNickname = friendMember.nickname,
                friendBio = memberInfo?.bio,
                friendProfileImageUrl = memberInfo?.profileImageUrl
            )
        }
    }
}
