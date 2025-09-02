package com.back.domain.member.member.dto.request

import jakarta.validation.constraints.NotBlank

data class PasswordCheckRequestDto(
    @field:NotBlank(message = "비밀번호는 필수 입력값입니다.")
    val password: String
)
