package com.back.domain.club.clubMember.controller

import com.back.domain.club.clubMember.dtos.MyClubList
import com.back.domain.club.clubMember.dtos.MyInfoInClub
import com.back.domain.club.clubMember.dtos.SimpleClubInfo
import com.back.domain.club.clubMember.service.ClubMemberService
import com.back.domain.club.clubMember.service.MyClubService
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import com.back.global.rsData.RsData.Companion.of
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.*

/**
 * 클럽 관련 API를 제공하는 컨트롤러
 * 유저 본인 입장에서 클럽 멤버 정보를 관리하는 기능을 포함한다
 */
@RestController
@RequestMapping("/api/v1/my-clubs")
@Tag(name = "MyClubController", description = "유저 본인 클럽 관련 API")
class ApiV1MyClubController(
    private val myClubService : MyClubService,
    private val clubMemberService : ClubMemberService,
    private val rq : Rq
){
    @GetMapping
    @Operation(summary = "내 클럽 목록 조회")
    fun getMyClubs(): RsData<MyClubList> {
        val myClubList = myClubService.getMyClubs()
        return of(
            200,
            "내 클럽 목록을 조회했습니다.",
            myClubList
        )
    }

    @GetMapping("{clubId}")
    @Operation(summary = "클럽에서 내 정보 조회")
    fun getMyClubInfo(
        @PathVariable clubId: Long
    ): RsData<MyInfoInClub> {
        // 클럽 멤버 정보를 조회하는 서비스 메서드를 호출
        val clubMember = myClubService.getMyClubInfo(clubId)

        // 조회된 클럽 멤버 정보를 응답으로 반환
        return of(
            200,
            "클럽 멤버 정보를 조회했습니다.",
            MyInfoInClub(
                clubMember.id!!,
                clubMember.club!!.id!!,
                clubMember.club!!.name,
                clubMember.role,
                clubMember.state
            )
        )
    }


    @PatchMapping("{clubId}/join")
    @Operation(summary = "클럽 초대 수락")
    fun acceptClubInvitation(
        @PathVariable clubId: Long
    ): RsData<SimpleClubInfo> {
        // 클럽 초대 수락 로직을 처리하는 서비스 메서드를 호출
        val selectedClub = myClubService.handleClubInvitation(clubId, true)

        // 성공적으로 초대를 수락/거절한 경우 응답 반환
        return of(
            200,
            "클럽 초대를 수락했습니다.",
            SimpleClubInfo(
                selectedClub.id!!,
                selectedClub.name
            )
        )
    }

    @DeleteMapping("{clubId}/invitation")
    @Operation(summary = "클럽 초대 거절")
    fun rejectClubInvitation(
        @PathVariable clubId: Long
    ): RsData<SimpleClubInfo> {
        // 클럽 초대 거절 로직을 처리하는 서비스 메서드를 호출
        val selectedClub = myClubService.handleClubInvitation(clubId, false)

        // 성공적으로 초대를 수락/거절한 경우 응답 반환
        return of(
            200,
            "클럽 초대를 거절했습니다.",
            SimpleClubInfo(
                selectedClub.id!!,
                selectedClub.name
            )
        )
    }

    @PostMapping("{clubId}/apply")
    @Operation(summary = "클럽 가입 신청")
    fun applyForPublicClub(
        @PathVariable clubId: Long
    ): RsData<SimpleClubInfo> {
        // 클럽 가입 신청 로직을 처리하는 서비스 메서드를 호출
        val selectedClub = myClubService.applyForClub(clubId)

        // 성공적으로 클럽 가입 신청을 한 경우 응답 반환
        return of(
            200,
            "클럽 가입 신청을 완료했습니다.",
            SimpleClubInfo(
                selectedClub.id!!,
                selectedClub.name
            )
        )
    }

    @DeleteMapping("{clubId}/apply")
    @Operation(summary = "클럽 가입 신청 취소")
    fun cancelClubApplication(
        @PathVariable clubId: Long
    ): RsData<SimpleClubInfo> {
        // 클럽 가입 신청 취소 로직을 처리하는 서비스 메서드를 호출
        val selectedClub = myClubService.cancelClubApplication(clubId)

        // 성공적으로 클럽 가입 신청을 취소한 경우 응답 반환
        return of(
            200,
            "클럽 가입 신청을 취소했습니다.",
            SimpleClubInfo(
                selectedClub.id!!,
                selectedClub.name
            )
        )
    }

    @DeleteMapping("{clubId}/withdraw")
    @Operation(summary = "클럽 탈퇴")
    fun withdrawFromClub(
        @PathVariable clubId: Long
    ): RsData<SimpleClubInfo> {
        // 클럽 탈퇴 로직을 처리하는 서비스 메서드를 호출
        clubMemberService.withdrawMemberFromClub(clubId, rq!!.actor!!.id!!)

        // 성공적으로 클럽에서 탈퇴한 경우 응답 반환
        return of(
            200,
            "클럽에서 탈퇴했습니다.",
            null
        )
    }
}
