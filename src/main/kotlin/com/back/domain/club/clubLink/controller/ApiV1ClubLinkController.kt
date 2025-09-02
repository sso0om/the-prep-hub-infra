package com.back.domain.club.clubLink.controller

import com.back.domain.club.club.dtos.SimpleClubInfoResponse
import com.back.domain.club.clubLink.dtos.CreateClubLinkResponse
import com.back.domain.club.clubLink.service.ClubLinkService
import com.back.domain.member.member.entity.Member
import com.back.global.enums.ClubApplyResult
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import com.back.standard.util.orServiceThrow
import io.swagger.v3.oas.annotations.Operation
import jakarta.validation.constraints.Positive
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/clubs")
class ApiV1ClubLinkController(
    private val rq: Rq,
    private val clubLinkService: ClubLinkService
) {

    @PostMapping("/{clubId}/members/invitation-link")
    @Operation(summary = "클럽 초대 링크 생성")
    fun createClubLink(@PathVariable @Positive clubId: Long): RsData<CreateClubLinkResponse> {
        val user: Member = rq.actor.orServiceThrow("로그인 유저가 존재하지 않습니다.")
        val response: CreateClubLinkResponse = clubLinkService.createClubLink(user, clubId)
        return RsData(200, "클럽 초대 링크가 생성되었습니다.", response)
    }

    @GetMapping("/{clubId}/members/invitation-link")
    @Operation(summary = "클럽 초대 링크 반환")
    fun getExistingClubLink(@PathVariable @Positive clubId: Long): RsData<CreateClubLinkResponse> {
        val user: Member = rq.actor.orServiceThrow("로그인 유저가 존재하지 않습니다.")
        val response: CreateClubLinkResponse = clubLinkService.getExistingClubLink(user, clubId)
        return RsData(200, "클럽 초대 링크가 반환되었습니다.", response)
    }

    @PostMapping("/invitations/{token}/apply")
    @Operation(summary = "로그인 유저 - 초대 링크를 통한 비공개 클럽 가입 신청")
    fun applyToClubByInvitationToken(@PathVariable token: String): RsData<Any?> {
        val user: Member = rq.actor.orServiceThrow("로그인 유저가 존재하지 않습니다.")
        return when (clubLinkService.applyToPrivateClub(user, token)) {
            ClubApplyResult.SUCCESS -> RsData(200, "클럽 가입 신청이 성공적으로 완료되었습니다.", null)
            ClubApplyResult.ALREADY_JOINED -> RsData(400, "이미 이 클럽에 가입되어 있습니다.", null)
            ClubApplyResult.ALREADY_APPLYING -> RsData(400, "이미 가입 신청 중입니다.", null)
            ClubApplyResult.ALREADY_INVITED -> RsData(400, "이미 초대를 받은 상태입니다. 마이페이지에서 수락해주세요.", null)
            ClubApplyResult.TOKEN_EXPIRED -> RsData(400, "초대 토큰이 만료되었습니다.", null)
            ClubApplyResult.TOKEN_INVALID -> RsData(400, "초대 토큰이 유효하지 않습니다.", null)
        }
    }

    @GetMapping("/invitations/{token}")
    @Operation(summary = "클럽 초대 링크용 클럽 정보 반환")
    fun getClubInfoByInvitationToken(@PathVariable token: String): RsData<SimpleClubInfoResponse> {
        val response: SimpleClubInfoResponse = clubLinkService.getClubInfoByInvitationToken(token)
        return RsData(200, "클럽 정보가 반환되었습니다.", response)
    }
}
