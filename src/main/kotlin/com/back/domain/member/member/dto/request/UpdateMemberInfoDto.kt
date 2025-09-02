package com.back.domain.member.member.dto.request

data class UpdateMemberInfoDto(
    val nickname: String? = null,
    val password: String? = null,
    val bio: String? = null
)
