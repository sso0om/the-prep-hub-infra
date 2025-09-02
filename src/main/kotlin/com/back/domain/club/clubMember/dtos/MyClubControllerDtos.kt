package com.back.domain.club.clubMember.dtos

import com.back.global.enums.ClubCategory
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.enums.EventType
import java.time.LocalDate


/**
 * 클럽 초대 수락 응답 DTO
 * 클럽 ID와 클럽 이름을 포함
 */
 data class SimpleClubInfo(
    val clubId: Long,
    val clubName: String
 )

/**
 * 클럽 내 내 정보 조회 응답 DTO
 * 클럽 멤버 ID, 클럽 ID, 클럽 이름, 역할, 상태를 포함
 */
 data class MyInfoInClub(
    val clubMemberId: Long,
    val clubId: Long,
    val clubName: String,
    val role: ClubMemberRole,
    val state: ClubMemberState
 )

 data class MyClubList(
     val clubs: MutableList<ClubListItem>
 )

data class ClubListItem(
    val clubId: Long,
    val clubName: String,
    val bio: String?,
    val category: ClubCategory,
    val imageUrl: String?,
    val mainSpot: String,
    val eventType: EventType,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isPublic: Boolean,
    val myRole: ClubMemberRole,
    val myState: ClubMemberState
)
