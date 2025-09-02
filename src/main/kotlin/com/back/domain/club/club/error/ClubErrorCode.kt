package com.back.domain.club.club.error

import com.back.global.exception.ErrorCode

enum class ClubErrorCode(
    override val status: Int,
    override val message: String
) : ErrorCode {
    // 404
    CLUB_NOT_FOUND(404, "모임을 찾을 수 없습니다."),
    CLUB_MEMBER_NOT_FOUND(404, "모임 참여자를 찾을 수 없습니다.");
}
