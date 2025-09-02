package com.back.domain.schedule.schedule.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class ScheduleCreateReqBody(
    @Schema(description = "모임 ID")
    @field:NotNull
    val clubId: Long,

    @Schema(description = "일정 제목")
    @field:NotBlank
    val title: String,

    @Schema(description = "일정 내용")
    @field:NotBlank
    val content: String,

    @Schema(description = "일정 시작일")
    @field:NotNull
    val startDate: LocalDateTime,

    @Schema(description = "일정 종료일")
    @field:NotNull
    val endDate: LocalDateTime,

    @Schema(description = "일정 장소")
    @field:NotBlank
    val spot: String
)
