package com.back.domain.club.clubMember.controller

import com.back.domain.club.clubMember.dtos.ClubMemberRegisterRequest
import com.back.domain.club.clubMember.dtos.ClubMemberResponse
import com.back.domain.club.clubMember.dtos.ClubMemberRoleChangeRequest
import com.back.domain.club.clubMember.service.ClubMemberService
import com.back.global.rsData.RsData
import com.back.global.rsData.RsData.Companion.of
import com.back.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * 클럽 멤버 관련 API를 제공하는 컨트롤러
 * 클럽장 입장에서 멤버를 관리하는 기능을 포함한다
 */
@RestController
@RequestMapping("/api/v1/clubs/{clubId}/members")
@Tag(name = "ClubMemberController", description = "클럽 멤버 관련 API")
class ApiV1ClubMemberController(
    private val clubMemberService: ClubMemberService
){

    @PostMapping
    @Operation(summary = "클럽에 멤버 추가")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubHost(#clubId, #user.id)")
    fun addMembersToClub(
        @PathVariable clubId: Long,
        @RequestBody reqBody: @Valid ClubMemberRegisterRequest,
        @AuthenticationPrincipal user: SecurityUser?

    ): RsData<Void> {
        clubMemberService.addMembersToClub(clubId, reqBody)

        return of(201, "클럽에 멤버가 추가됐습니다.", null)
    }

    @DeleteMapping("/{memberId}")
    @Operation(summary = "클럽에서 멤버 탈퇴")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubHost(#clubId, #user.id) || @clubAuthorizationChecker.isSelf(#memberId, #user.id)")
    fun withdrawMemberFromClub(
        @PathVariable clubId: Long,
        @PathVariable memberId: Long,
        @AuthenticationPrincipal user: SecurityUser?
    ): RsData<Void> {
        clubMemberService.withdrawMemberFromClub(clubId, memberId)

        return of(200, "클럽에서 멤버가 탈퇴됐습니다.", null)
    }

    @PutMapping("/{memberId}/role")
    @Operation(summary = "클럽 멤버 권한 변경")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubHost(#clubId, #user.id)")
    fun changeMemberRole(
        @PathVariable clubId: Long,
        @PathVariable memberId: Long,
        @RequestBody reqBody: @Valid ClubMemberRoleChangeRequest,
        @AuthenticationPrincipal user: SecurityUser?
    ): RsData<Void> {
        clubMemberService.changeMemberRole(clubId, memberId, reqBody.role)

        return of(200, "멤버의 권한이 변경됐습니다.", null)
    }

    @GetMapping
    @Operation(summary = "클럽 멤버 목록 조회")
    @PreAuthorize("@clubAuthorizationChecker.isClubMember(#clubId, #user.id)")
    fun getClubMembers(
        @PathVariable clubId: Long,
        @RequestParam(required = false) state: String?,  // Optional: 상태 필터링
        @AuthenticationPrincipal user: SecurityUser?
    ): RsData<ClubMemberResponse> {
        val clubMemberResponse = clubMemberService.getClubMembers(clubId, state)

        return of(200, "클럽 멤버 목록이 조회됐습니다.", clubMemberResponse)
    }

    @PatchMapping("/{memberId}/approval")
    @Operation(summary = "클럽 가입 신청 승인")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubHost(#clubId, #user.id)")
    fun approveMemberApplication(
        @PathVariable clubId: Long,
        @PathVariable memberId: Long,
        @AuthenticationPrincipal user: SecurityUser?
    ): RsData<Void> {
        clubMemberService.handleMemberApplication(clubId, memberId, true)

        return of(200, "가입 신청이 승인됐습니다.", null)
    }

    @DeleteMapping("/{memberId}/approval")
    @Operation(summary = "클럽 가입 신청 거절")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubHost(#clubId, #user.id)")
    fun rejectMemberApplication(
        @PathVariable clubId: Long,
        @PathVariable memberId: Long,
        @AuthenticationPrincipal user: SecurityUser?
    ): RsData<Void> {
        clubMemberService.handleMemberApplication(clubId, memberId, false)

        return of(200, "가입 신청이 거절됐습니다.", null)
    }
}
