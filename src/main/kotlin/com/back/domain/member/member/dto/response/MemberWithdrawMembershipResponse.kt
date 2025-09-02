package com.back.domain.member.member.dto.response

import io.swagger.v3.oas.annotations.media.Schema

data class MemberWithdrawMembershipResponse(
    @field:Schema(description = "회원 닉네임", example = "testUser1")
    val nickname: String,

    @field:Schema(description = "회원 태그", example = "2345")
    val tag: String
)
