package com.back.domain.checkList.checkList.dto

import com.back.domain.checkList.checkList.entity.CheckListItem
import com.back.domain.checkList.itemAssign.dto.ItemAssignDto
import com.back.global.enums.CheckListItemCategory

data class CheckListItemDto(
    val id: Long?,
    val content: String,
    val category: CheckListItemCategory,
    val sequence: Int,
    val isChecked: Boolean,
    val itemAssigns: List<ItemAssignDto>
) {
    /**
     * CheckListItem 엔티티를 DTO로 변환하는 보조 생성자
     */
    constructor(checkListItem: CheckListItem) : this(
        id = checkListItem.id,
        content = checkListItem.content,
        category = checkListItem.category,
        sequence = checkListItem.sequence,
        isChecked = checkListItem.isChecked,
        // itemAssigns가 null이면 빈 리스트를, 아니면 DTO 리스트로 변환
        itemAssigns = checkListItem.itemAssigns?.map { ItemAssignDto(it) } ?: emptyList()
    )
}