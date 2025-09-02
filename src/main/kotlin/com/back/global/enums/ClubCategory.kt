package com.back.global.enums

import com.back.global.exception.ServiceException

enum class ClubCategory(val description: String) {
    STUDY("스터디"),
    HOBBY("취미"),
    SPORTS("운동"),
    TRAVEL("여행"),
    CULTURE("문화"),
    FOOD("음식"),
    PARTY("파티"),
    WORK("업무"),
    OTHER("기타");

    companion object {
        @JvmStatic
        fun fromString(category: String): ClubCategory {
            val key = category.trim()
            return values().find { it.name.equals(key, ignoreCase = true) }
                ?: throw ServiceException(400, "Unknown Item category: $category")
        }
    }
}