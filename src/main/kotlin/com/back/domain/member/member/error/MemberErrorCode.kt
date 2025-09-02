package com.back.domain.member.member.error

import com.back.global.exception.ErrorCode

enum class MemberErrorCode(
    override val status: Int,
    override val message: String
) : ErrorCode {
    MEMBER_NOT_FOUND(404, "회원을 찾지 못했습니다.")
}
