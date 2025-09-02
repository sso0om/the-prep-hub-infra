package com.back.domain.schedule.schedule.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * 날짜 및 시간 범위를 나타내는 레코드 클래스
 * 시작 날짜 및 시간과 종료 날짜 및 시간을 포함
 */
data class DateTimeRange(
    @Schema(description = "시작 날짜 및 시간")
    val startDateTime: LocalDateTime,

    @Schema(description = "종료 날짜 및 시간")
    val endDateTime: LocalDateTime
)
