package com.back.domain.club.club.dtos

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat

/**
 * 클럽 생성 요청을 위한 DTO 클래스입니다.
 * 클럽의 이름, 소개, 카테고리, 주요 장소, 최대 인원 수, 이벤트 유형, 시작일, 종료일,
 * 공개 여부, 리더 ID 및 클럽 멤버 정보를 포함합니다.
 */
data class CreateClubRequest(
    @field:NotBlank
    val name: String,

    @field:NotBlank
    val bio: String,

    @field:NotBlank
    val category: String,

    @field:NotBlank
    val mainSpot: String,

    @field:Min(1)
    val maximumCapacity: Int,

    @field:NotBlank
    val eventType: String,

    @field:NotBlank
    @field:DateTimeFormat(pattern = "yyyy-MM-dd")
    val startDate: String,

    @field:NotBlank
    @field:DateTimeFormat(pattern = "yyyy-MM-dd")
    val endDate: String,

    @field:NotNull
    val isPublic: Boolean,

    var clubMembers: List<CreateClubRequestMemberInfo>
)

data class CreateClubRequestMemberInfo(
    @field:NotNull
    val id: Long,

    @field:NotBlank
    val role: String
)

/**
 * 클럽 수정 요청을 위한 DTO 클래스입니다.
 */
data class UpdateClubRequest(
    val name: String?,
    val bio: String?,
    val category: String?,
    val mainSpot: String?,
    val maximumCapacity: Int?,
    val recruitingStatus: Boolean?,
    val eventType: String?,
    val startDate: String?,
    val endDate: String?,
    var isPublic: Boolean?
)

/**
 * 클럽 생성 응답 DTO
 */
data class ClubResponse(
    val clubId: Long,
    val leaderId: Long
)

/**
 * 클럽 정보 조회 응답 DTO
 */
data class ClubInfoResponse(
    val clubId: Long,
    val name: String,
    val bio: String,
    val category: String,
    val mainSpot: String,
    val maximumCapacity: Int,
    val recruitingStatus: Boolean,
    val eventType: String,
    val startDate: String,
    val endDate: String,
    val isPublic: Boolean,
    val imageUrl: String?,
    val leaderId: Long,
    val leaderName: String
)

data class SimpleClubInfoResponse(
    val clubId: Long,
    val name: String,
    val category: String,
    val imageUrl: String?,
    val mainSpot: String,
    val eventType: String,
    val startDate: String,
    val endDate: String,
    val leaderId: Long,
    val leaderName: String
)

data class SimpleClubInfoWithoutLeader(
    val clubId: Long,
    val name: String,
    val category: String,
    val imageUrl: String?,
    val mainSpot: String,
    val eventType: String,
    val startDate: String,
    val endDate: String,
    val bio: String
)
