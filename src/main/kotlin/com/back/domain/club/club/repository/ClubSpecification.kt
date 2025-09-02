package com.back.domain.club.club.repository

import com.back.domain.club.club.entity.Club
import com.back.global.enums.ClubCategory
import com.back.global.enums.EventType
import org.springframework.data.jpa.domain.Specification
import org.springframework.util.StringUtils

object ClubSpecification {

    // 이름(name)으로 부분 일치 검색
    @JvmStatic
    fun likeName(name: String?): Specification<Club>? {
        if (!StringUtils.hasText(name)) {
            return null
        }
        return Specification { root, _, cb ->
            cb.like(root.get("name"), "%$name%")
        }
    }

    // 지역(mainSpot)으로 부분 일치 검색
    @JvmStatic
    fun likeMainSpot(mainSpot: String?): Specification<Club>? {
        if (!StringUtils.hasText(mainSpot)) {
            return null
        }
        return Specification { root, _, cb ->
            cb.like(root.get("mainSpot"), "%$mainSpot%")
        }
    }

    // 카테고리(category)로 완전 일치 검색
    @JvmStatic
    fun equalCategory(category: ClubCategory?): Specification<Club>? {
        if (category == null) {
            return null
        }
        return Specification { root, _, cb ->
            cb.equal(root.get<ClubCategory>("category"), category)
        }
    }

    // 모집 유형(eventType)으로 완전 일치 검색
    @JvmStatic
    fun equalEventType(eventType: EventType?): Specification<Club>? {
        if (eventType == null) {
            return null
        }
        return Specification { root, _, cb ->
            cb.equal(root.get<EventType>("eventType"), eventType)
        }
    }

    // 공개된 클럽만 조회하는 기본 조건
    @JvmStatic
    fun isPublic(): Specification<Club> {
        return Specification { root, _, cb ->
            cb.isTrue(root.get("isPublic"))
        }
    }
}
