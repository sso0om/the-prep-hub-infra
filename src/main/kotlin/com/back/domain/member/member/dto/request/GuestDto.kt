package com.back.domain.member.member.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class GuestDto(
    @field:NotBlank(message = "닉네임은 필수 입력값입니다.")
    val nickname: String,

    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    val password: String,

    @field:NotNull(message = "클럽 id는 필수 입력값입니다.")
    val clubId: Long
)
