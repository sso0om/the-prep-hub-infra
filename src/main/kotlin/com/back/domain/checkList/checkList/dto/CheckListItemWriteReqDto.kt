package com.back.domain.checkList.checkList.dto

import com.back.domain.checkList.itemAssign.dto.ItemAssignReqDto
import com.back.global.enums.CheckListItemCategory
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CheckListItemWriteReqDto(
    @field:NotBlank(message = "내용은 필수입니다.")
    val content: String,

    @field:NotNull(message = "카테고리는 필수입니다.")
    val category: CheckListItemCategory,

    val sequence: Int,

    val isChecked: Boolean,

    @field:Valid
    val itemAssigns: List<ItemAssignReqDto>
)