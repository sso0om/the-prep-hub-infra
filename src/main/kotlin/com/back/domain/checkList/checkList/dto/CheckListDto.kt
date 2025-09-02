package com.back.domain.checkList.checkList.dto

import com.back.domain.checkList.checkList.entity.CheckList
import com.back.domain.schedule.schedule.dto.response.ScheduleDto

data class CheckListDto(
    val id: Long?,
    val isActive: Boolean,
    val schedule: ScheduleDto,
    val checkListItems: List<CheckListItemDto>
) {
    /**
     * CheckList 엔티티를 DTO로 변환하는 보조 생성자
     */
    constructor(checkList: CheckList) : this(
        id = checkList.id,
        isActive = checkList.isActive,
        schedule = ScheduleDto.from(checkList.schedule),
        // 1. checkListItems가 null이면 빈 리스트를, 아니면 DTO 리스트로 변환
        checkListItems = checkList.checkListItems
            ?.map { CheckListItemDto(it) } ?: emptyList()
    )
}