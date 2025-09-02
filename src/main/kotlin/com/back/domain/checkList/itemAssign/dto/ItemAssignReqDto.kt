package com.back.domain.checkList.itemAssign.dto

import jakarta.validation.constraints.NotNull

data class ItemAssignReqDto(
    @field:NotNull(message = "클럽 멤버 ID는 필수입니다.")
    val clubMemberId: Long,
    val isChecked: Boolean
)