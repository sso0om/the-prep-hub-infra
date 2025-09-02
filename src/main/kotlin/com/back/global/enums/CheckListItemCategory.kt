package com.back.global.enums

import com.back.global.exception.ServiceException

enum class CheckListItemCategory(val description: String) {
    // 준비물
    PREPARATION("준비물"),
    // 예약
    RESERVATION("예약"),
    // 사전 작업
    PRE_WORK("사전 작업"),
    // 기타
    ETC("기타");

    companion object {
            @JvmStatic
            fun fromString(category: String): CheckListItemCategory {
                val key = category.trim()
                return values().find { it.name.equals(key, ignoreCase = true) }
                    ?: throw ServiceException(400, "Unknown Item category: $category")
            }
        }
}