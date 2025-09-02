package com.back.domain.preset.preset.dto

import com.back.domain.preset.preset.entity.PresetItem
import com.back.global.enums.CheckListItemCategory

data class PresetItemDto(
    val id: Long?,
    val content: String,
    val category: CheckListItemCategory,
    val sequence: Int
) {
    /**
     * PresetItem 엔티티를 DTO로 변환하는 보조 생성자
     */
    constructor(presetItem: PresetItem) : this(
        id = presetItem.id,
        content = presetItem.content,
        category = presetItem.category,
        sequence = presetItem.sequence
    )
}