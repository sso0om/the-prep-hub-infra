package com.back.global.enums

import com.back.global.exception.ServiceException

enum class EventType(val description: String) {
    // 일회성, 단기, 장기
    ONE_TIME("일회성"), // 한 번만 열리는 모임
    SHORT_TERM("단기"), // 특정 기간 동안 열리는 모임
    LONG_TERM("장기"); // 종료일 없이 지속적으로 열리는 모임

    companion object {
        @JvmStatic
        fun fromString(eventType: String): EventType =
            values().firstOrNull { it.name.equals(eventType.trim(), ignoreCase = true) }
                ?: throw ServiceException(400, "Unknown event type: $eventType")
    }
}