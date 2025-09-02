package com.back.domain.schedule.schedule.dto.response

import com.back.domain.schedule.schedule.entity.Schedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ScheduleWithClubDto(
    @Schema(description = "일정 ID")
    val id: Long,

    @Schema(description = "일정 제목")
    val title: String,

    @Schema(description = "일정 시작일")
    val startDate: LocalDateTime,

    @Schema(description = "일정 종료일")
    val endDate: LocalDateTime,

    @Schema(description = "모임 ID")
    val clubId: Long,

    @Schema(description = "모임명")
    val clubName: String,

    @Schema(description = "체크리스트 ID", nullable = true)
    val checkListId: Long?,
) {
    companion object {
        fun from(schedule: Schedule): ScheduleWithClubDto {
            return ScheduleWithClubDto(
                id = schedule.id!!,
                title = schedule.title,
                startDate = schedule.startDate,
                endDate = schedule.endDate,
                clubId = schedule.club.id !!,
                clubName = schedule.club.name,
                checkListId = schedule.checkList?.id
            )
        }
    }
}
