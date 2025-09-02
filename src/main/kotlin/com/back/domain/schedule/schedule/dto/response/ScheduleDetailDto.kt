package com.back.domain.schedule.schedule.dto.response

import com.back.domain.schedule.schedule.entity.Schedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ScheduleDetailDto(
    @Schema(description = "일정 ID")
    val id: Long,

    @Schema(description = "일정 제목")
    val title: String,

    @Schema(description = "일정 내용")
    val content: String,

    @Schema(description = "일정 시작일")
    val startDate: LocalDateTime,

    @Schema(description = "일정 종료일")
    val endDate: LocalDateTime,

    @Schema(description = "일정 장소")
    val spot: String,

    @Schema(description = "모임 ID")
    val clubId: Long,

    @Schema(description = "체크리스트 ID")
    val checkListId: Long?,
) {
    companion object {
        fun from(schedule: Schedule): ScheduleDetailDto {
            return ScheduleDetailDto(
                id = schedule.id!!,
                title = schedule.title,
                content = schedule.content,
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                spot = schedule.spot,
                clubId = schedule.club.id !!,
                checkListId = schedule.checkList?.id
            )
        }
    }
}
