package com.back.domain.preset.preset.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank

data class PresetWriteReqDto(
    @field:NotBlank(message = "프리셋 이름은 필수입니다")
    val name: String,

    @field:Valid
    val presetItems: List<PresetItemWriteReqDto>
)