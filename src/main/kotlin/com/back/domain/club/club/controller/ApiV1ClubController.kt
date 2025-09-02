package com.back.domain.club.club.controller

import com.back.domain.club.club.dtos.*
import com.back.domain.club.club.service.ClubService
import com.back.global.enums.ClubCategory
import com.back.global.enums.EventType
import com.back.global.rsData.RsData
import com.back.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@RestController
@RequestMapping("/api/v1/clubs")
@Tag(name = "ClubController", description = "클럽 관련 API")
class ApiV1ClubController (
    private val clubService : ClubService
){
    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "클럽 생성")
    @Throws(IOException::class)
    fun createClub(
        @RequestPart("data") reqBody: @Valid CreateClubRequest,
        @RequestPart(value = "image", required = false) image: MultipartFile?
    ): RsData<ClubResponse?> {
        val club = clubService.createClub(reqBody, image)

        return RsData<ClubResponse?>(
            201, "클럽이 생성됐습니다.",
            ClubResponse(
                club.id ?: throw IllegalStateException("클럽 ID가 존재하지 않습니다."),
                club.leaderId ?: throw IllegalStateException("클럽장 ID가 존재하지 않습니다.")
            )
        )
    }

    @PatchMapping("/{clubId}")
    @Operation(summary = "클럽 수정")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubHost(#clubId, #user.id)")
    @Throws(IOException::class)
    fun updateClubInfo(
        @PathVariable clubId: Long,
        @RequestPart("data") reqBody: @Valid UpdateClubRequest,
        @RequestPart(value = "image", required = false) image: MultipartFile?,
        @AuthenticationPrincipal user: SecurityUser?
    ): RsData<ClubResponse?> {
        val club = clubService.updateClub(clubId, reqBody, image)

        return RsData<ClubResponse?>(
            200, "클럽 정보가 수정됐습니다.",
            ClubResponse(
                club.id!!,
                club.leaderId!!
            )
        )
    }

    @DeleteMapping("/{clubId}")
    @Operation(summary = "클럽 삭제")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubHost(#clubId, #user.id)")
    fun deleteClub(
        @PathVariable clubId: Long,
        @AuthenticationPrincipal user: SecurityUser?
    ): RsData<Void?> {
        clubService.deleteClub(clubId)
        return RsData<Void?>(204, "클럽이 삭제됐습니다.", null)
    }

    @GetMapping("/{clubId}")
    @Operation(summary = "클럽 정보 조회")
    fun getClubInfo(@PathVariable clubId: Long): RsData<ClubInfoResponse?> {
        val info = clubService.getClubInfo(clubId)
        return RsData<ClubInfoResponse?>(200, "클럽 정보가 조회됐습니다.", info)
    }

    @GetMapping("/public")
    @Operation(summary = "공개 클럽 목록 조회 (페이징 가능)")
    fun getPublicClubs(
        @ParameterObject pageable: Pageable,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) mainSpot: String?,
        @RequestParam(required = false) category: ClubCategory?,
        @RequestParam(required = false) eventType: EventType?
    ): RsData<Page<SimpleClubInfoWithoutLeader?>?> {
        val response = clubService.getPublicClubs(pageable, name, mainSpot, category, eventType)
        return RsData<Page<SimpleClubInfoWithoutLeader?>?>(200, "공개 클럽 목록이 조회됐습니다.", response)
    }
}
