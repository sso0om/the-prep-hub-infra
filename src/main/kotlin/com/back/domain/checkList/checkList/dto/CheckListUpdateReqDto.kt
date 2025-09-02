package com.back.domain.checkList.checkList.dto

import jakarta.validation.Valid

data class CheckListUpdateReqDto(
    @field:Valid
    val checkListItems: List<CheckListItemWriteReqDto>
)
