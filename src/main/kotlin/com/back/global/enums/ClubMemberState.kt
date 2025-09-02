package com.back.global.enums

import com.back.global.exception.ServiceException

enum class ClubMemberState(val description: String) {
    INVITED("초대됨"),
    JOINING("가입 중"),
    APPLYING("가입 신청"),
    WITHDRAWN("탈퇴");

    companion object {
        @JvmStatic
        fun fromString(state: String): ClubMemberState =
            values().firstOrNull { it.name.equals(state.trim(), ignoreCase = true) }
                ?: throw ServiceException(400, "Unknown Member state: $state")
    }
}