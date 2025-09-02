package com.back.domain.preset.preset.dto

import com.back.domain.preset.preset.entity.Preset

data class PresetDto(
    val id: Long?,
    val name: String,
    val presetItems: List<PresetItemDto>
) {
    /**
     * Preset 엔티티를 DTO로 변환하는 보조 생성자
     */
    constructor(preset: Preset) : this(
        id = preset.id,
        name = requireNotNull(preset.name) { "Preset.name must not be null" },
        // presetItems가 null인 경우 빈 리스트로 방어
        presetItems = (preset.presetItems ?: emptyList()).map(::PresetItemDto)
    )
}