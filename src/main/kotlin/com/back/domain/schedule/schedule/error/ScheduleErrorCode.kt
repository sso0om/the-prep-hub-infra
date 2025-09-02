package com.back.domain.schedule.schedule.error

import com.back.global.exception.ErrorCode

enum class ScheduleErrorCode(
    override val status: Int,
    override val message: String
) : ErrorCode {
    // 404
    SCHEDULE_NOT_FOUND(404, "일정을 찾을 수 없습니다."),
    CLUB_NOT_FOUND(404, "모임을 찾을 수 없습니다."),

    // 400
    SCHEDULE_INVALID_DATE(400, "유효하지 않은 날짜입니다."),
    SCHEDULE_INVALID_TIME(400, "유효하지 않은 시간입니다."),
    SCHEDULE_INVALID_MEMBER(400, "유효하지 않은 회원입니다."),

    // 403
    SCHEDULE_ACCESS_DENIED(403, "해당 일정에 접근할 권한이 없습니다."),

    // 409
    SCHEDULE_CONFLICT(409, "일정이 충돌합니다.");
}
