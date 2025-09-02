package com.back.global.enums

import com.back.global.exception.ServiceException

enum class ClubMemberRole(val description: String) {
    PARTICIPANT("일반 회원"),
    MANAGER("관리자"),
    HOST("소유자");

    companion object {
        @JvmStatic
        fun fromString(role: String): ClubMemberRole {
            val key = role.trim()
            return values().find { it.name.equals(key, ignoreCase = true) }
                ?: throw ServiceException(400, "Unknown Member role: $role")
        }
    }
}