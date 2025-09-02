package com.back.domain.schedule.schedule.controller

import com.back.domain.schedule.schedule.dto.request.ScheduleCreateReqBody
import com.back.domain.schedule.schedule.dto.request.ScheduleUpdateReqBody
import com.back.domain.schedule.schedule.dto.response.ScheduleDetailDto
import com.back.domain.schedule.schedule.dto.response.ScheduleDto
import com.back.domain.schedule.schedule.dto.response.ScheduleWithClubDto
import com.back.domain.schedule.schedule.service.ScheduleService
import com.back.global.rsData.RsData
import com.back.global.rsData.RsData.Companion.of
import com.back.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "ApiV1ScheduleController", description = "일정 컨트롤러")
class ApiV1ScheduleController(
    private val scheduleService: ScheduleService
) {
    @GetMapping("/clubs/{clubId}")
    @Operation(summary = "모임의 일정 목록 조회", description = "모임의 일정 목록 조회는 모임의 참여자만 가능")
    @PreAuthorize("@clubAuthorizationChecker.isClubMember(#clubId, #user.getId())")
    fun getClubSchedules(
        @PathVariable clubId: Long,
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?,
        @AuthenticationPrincipal user: SecurityUser
    ): RsData<List<ScheduleDto>> {
        val schedules = scheduleService.getClubSchedules(clubId, startDate, endDate)

        return of(
            200,
            "일정 목록이 조회되었습니다.",
            schedules
        )
    }

    @GetMapping("/{scheduleId}")
    @Operation(summary = "일정 조회", description = "일정 조회는 모임의 참여자만 가능")
    @PreAuthorize("@scheduleAuthorizationChecker.isClubMember(#scheduleId, #user.getId())")
    fun getSchedule(
        @PathVariable scheduleId: Long,
        @AuthenticationPrincipal user: SecurityUser
    ): RsData<ScheduleDetailDto> {
        val schedule = scheduleService.getActiveScheduleById(scheduleId)

        return of(
            200,
            "일정이 조회되었습니다.",
            schedule
        )
    }

    @PostMapping
    @Operation(summary = "일정 생성", description = "일정 생성은 호스트 또는 매니저 권한이 있는 사용자만 가능")
    @PreAuthorize("@clubAuthorizationChecker.isActiveClubManagerOrHost(#reqBody.clubId, #user.getId())")
    fun createSchedule(
        @RequestBody @Valid reqBody: ScheduleCreateReqBody,
        @AuthenticationPrincipal user: SecurityUser
    ): RsData<ScheduleDetailDto> {
        val schedule = scheduleService.createSchedule(reqBody)

        return of(
            201,
            "일정이 생성되었습니다.",
            schedule
        )
    }

    @PutMapping("/{scheduleId}")
    @Operation(summary = "일정 수정", description = "일정 수정은 호스트 또는 매니저 권한이 있는 사용자만 가능")
    @PreAuthorize("@scheduleAuthorizationChecker.isActiveClubManagerOrHost(#scheduleId, #user.getId())")
    fun modifySchedule(
        @PathVariable scheduleId: Long,
        @RequestBody @Valid reqBody: ScheduleUpdateReqBody,
        @AuthenticationPrincipal user: SecurityUser
    ): RsData<ScheduleDetailDto> {
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)
        scheduleService.modifySchedule(schedule, reqBody)

        return of(
            200,
            "일정이 수정되었습니다.",
            ScheduleDetailDto.from(schedule)
        )
    }

    @DeleteMapping("/{scheduleId}")
    @Operation(summary = "일정 삭제", description = "일정 삭제는 호스트 또는 매니저 권한이 있는 사용자만 가능")
    @PreAuthorize("@scheduleAuthorizationChecker.isActiveClubManagerOrHost(#scheduleId, #user.getId())")
    fun deleteSchedule(
        @PathVariable scheduleId: Long,
        @AuthenticationPrincipal user: SecurityUser
    ): RsData<Void> {
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)
        scheduleService.deleteSchedule(schedule)

        return of(
            200,
            "일정이 삭제되었습니다."
        )
    }

    @GetMapping("/me")
    @Operation(summary = "나의 일정 목록 조회")
    fun getMySchedules(
        @AuthenticationPrincipal user: SecurityUser,
        @RequestParam(value = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") startDate: LocalDate?,
        @RequestParam(value = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") endDate: LocalDate?
    ): RsData<List<ScheduleWithClubDto>> {
        val mySchedules = scheduleService.getMySchedules(user.id, startDate, endDate)
        return of(
            200,
            "나의 일정 목록이 조회되었습니다.",
            mySchedules
        )
    }
}
