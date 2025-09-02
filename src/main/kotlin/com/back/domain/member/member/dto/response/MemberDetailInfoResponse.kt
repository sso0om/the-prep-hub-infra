package com.back.domain.member.member.dto.response

data class MemberDetailInfoResponse(
    val nickname: String,
    val email: String,
    val bio: String?,
    val profileImage: String?,
    val tag: String?
)
