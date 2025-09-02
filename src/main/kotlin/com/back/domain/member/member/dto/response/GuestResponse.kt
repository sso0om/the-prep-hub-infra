package com.back.domain.member.member.dto.response

data class GuestResponse(
    val nickname: String,
    val accessToken: String,
    val clubId: Long
)
