package com.back.domain.checkList.itemAssign.dto

import com.back.domain.checkList.itemAssign.entity.ItemAssign

data class ItemAssignDto(
    val id: Long?,
    val clubMemberId: Long?,
    val clubMemberName: String,
    val isChecked: Boolean
) {
    /**
     * ItemAssign 엔티티를 DTO로 변환하는 보조 생성자
     */
    constructor(itemAssign: ItemAssign) : this(
        id = itemAssign.id,
        clubMemberId = itemAssign.clubMember.id,
        clubMemberName = itemAssign.clubMember.member.nickname,
        isChecked = itemAssign.isChecked
    )
}