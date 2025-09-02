package com.back.domain.checkList.checkList.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull

data class CheckListWriteReqDto(
    @field:NotNull(message = "일정 ID는 필수입니다.")
    val scheduleId: Long,

    @field:Valid
    val checkListItems: List<CheckListItemWriteReqDto>
)