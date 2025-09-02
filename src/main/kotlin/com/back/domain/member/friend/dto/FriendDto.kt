package com.back.domain.member.friend.dto

import com.back.domain.member.friend.entity.Friend
import com.back.domain.member.friend.entity.FriendStatus
import com.back.domain.member.member.entity.Member
import io.swagger.v3.oas.annotations.media.Schema

data class FriendDto(
    @Schema(description = "친구 ID")
    val friendId: Long,

    @Schema(description = "친구(회원) ID")
    val friendMemberId: Long,

    @Schema(description = "친구(회원) 닉네임")
    val friendNickname: String,

    @Schema(description = "친구(회원) 자기소개")
    val friendBio: String?,

    @Schema(description = "친구(회원) 프로필 이미지 URL")
    val friendProfileImageUrl: String?,

    @Schema(description = "친구 관계")
    val status: FriendStatus
) {
    companion object {
        fun from(friend: Friend, friendMember: Member): FriendDto {
            val memberInfo = friendMember.getMemberInfo()
            return FriendDto(
                friendId = friend.id!!,
                friendMemberId = friendMember.id!!,
                friendNickname = friendMember.nickname,
                friendBio = memberInfo?.bio,
                friendProfileImageUrl = memberInfo?.profileImageUrl,
                status = friend.status
            )
        }
    }
}
