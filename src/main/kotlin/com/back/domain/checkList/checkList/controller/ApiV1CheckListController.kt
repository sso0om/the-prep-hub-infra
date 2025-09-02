package com.back.domain.checkList.checkList.controller

import com.back.domain.checkList.checkList.dto.CheckListDto
import com.back.domain.checkList.checkList.dto.CheckListUpdateReqDto
import com.back.domain.checkList.checkList.dto.CheckListWriteReqDto
import com.back.domain.checkList.checkList.service.CheckListService
import com.back.global.rsData.RsData
import com.back.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/**
 * RsData를 ResponseEntity로 변환하는 확장 함수
 * 컨트롤러의 반복적인 코드를 줄여줍니다.
 */
private fun <T> RsData<T>.toResponseEntity(): ResponseEntity<RsData<T>> =
    ResponseEntity
        .status(this.code)
        .body(this)

@RestController
@RequestMapping("/api/v1/checklists")
@Tag(name = "ApiV1CheckListController", description = "체크리스트 API V1 컨트롤러")
class ApiV1CheckListController(
    private val checkListService: CheckListService // 1. 생성자 주입
) {

    @PostMapping
    @Operation(summary = "체크리스트 생성")
    @PreAuthorize("@checkListAuthorizationChecker.isActiveClubManagerOrHostByScheduleId(#checkListWriteReqDto.scheduleId, #user.id)")
    fun write(
        @Valid @RequestBody checkListWriteReqDto: CheckListWriteReqDto,
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<RsData<CheckListDto>> {
        val rsData = checkListService.write(checkListWriteReqDto)
        return rsData.toResponseEntity() // 2. 확장 함수 사용
    }

    @GetMapping("/{checkListId}")
    @Operation(summary = "체크리스트 조회")
    @PreAuthorize("@checkListAuthorizationChecker.isClubMember(#checkListId, #user.id)")
    fun getCheckList(
        @PathVariable checkListId: Long,
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<RsData<CheckListDto>> =
        checkListService.getCheckList(checkListId).toResponseEntity() // 3. 단일 표현식 함수

    @PutMapping("/{checkListId}")
    @Operation(summary = "체크리스트 수정")
    @PreAuthorize("@checkListAuthorizationChecker.isActiveClubManagerOrHost(#checkListId, #user.id)")
    fun updateCheckList(
        @PathVariable checkListId: Long,
        @Valid @RequestBody checkListUpdateReqDto: CheckListUpdateReqDto,
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<RsData<CheckListDto>> =
        checkListService.updateCheckList(checkListId, checkListUpdateReqDto).toResponseEntity()

    @DeleteMapping("/{checkListId}")
    @Operation(summary = "체크리스트 삭제")
    @PreAuthorize("@checkListAuthorizationChecker.isActiveClubManagerOrHost(#checkListId, #user.id)")
    fun deleteCheckList(
        @PathVariable checkListId: Long,
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<RsData<Void>> { // 4. 명확한 반환 타입
        val rsData = checkListService.deleteCheckList(checkListId)
        return rsData.toResponseEntity()
    }

    @GetMapping("/group/{groupId}")
    @Operation(summary = "체크리스트 목록 조회")
    @PreAuthorize("@clubAuthorizationChecker.isClubMember(#groupId, #user.id)")
    fun getCheckListByGroupId(
        @PathVariable groupId: Long,
        @AuthenticationPrincipal user: SecurityUser
    ): ResponseEntity<RsData<List<CheckListDto>>> =
        checkListService.getCheckListByGroupId(groupId).toResponseEntity()
}