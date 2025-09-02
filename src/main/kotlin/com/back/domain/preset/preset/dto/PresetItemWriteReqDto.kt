package com.back.domain.preset.preset.dto

import com.back.global.enums.CheckListItemCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero

data class PresetItemWriteReqDto(
    @field:NotBlank(message = "내용은 필수입니다.")
    val content: String,

    @field:NotNull(message = "카테고리는 필수입니다.")
    val category: CheckListItemCategory,

    @field:PositiveOrZero(message = "sequence는 0 이상이어야 합니다.")
    val sequence: Int
)