package com.back.domain.schedule.schedule.service

import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.error.ClubErrorCode
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.schedule.schedule.dto.DateTimeRange
import com.back.domain.schedule.schedule.dto.request.ScheduleCreateReqBody
import com.back.domain.schedule.schedule.dto.request.ScheduleUpdateReqBody
import com.back.domain.schedule.schedule.dto.response.ScheduleDetailDto
import com.back.domain.schedule.schedule.dto.response.ScheduleDto
import com.back.domain.schedule.schedule.dto.response.ScheduleWithClubDto
import com.back.domain.schedule.schedule.entity.Schedule
import com.back.domain.schedule.schedule.error.ScheduleErrorCode
import com.back.domain.schedule.schedule.repository.ScheduleRepository
import com.back.global.exception.ServiceException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

@Service
class ScheduleService(
    private val scheduleRepository: ScheduleRepository,
    private val clubRepository: ClubRepository,
) {
    /**
     * 특정 모임의 일정 목록 조회
     * @param clubId
     * @return schedules
     */
    @Transactional(readOnly = true)
    fun getClubSchedules(clubId: Long, startDate: LocalDate?, endDate: LocalDate?): List<ScheduleDto> {
        val (startDateTime, endDateTime) = getDateTimeRange(startDate, endDate)

        // 활성화된 일정 중, 범위 내에 있는 목록을 시작 날짜 기준으로 오름차순 정렬하여 조회
        return scheduleRepository
            .findSchedulesByClubAndDateRange(clubId, startDateTime, endDateTime)
            .map { ScheduleDto.from(it) }
    }

    /**
     * 나의 모든 모임 일정 목록 조회 (달력 단위)
     * @param memberId
     * @param startDate
     * @param endDate
     * @return
     */
    @Transactional(readOnly = true)
    fun getMySchedules(memberId: Long, startDate: LocalDate?, endDate: LocalDate?): List<ScheduleWithClubDto> {
        val (startDateTime, endDateTime) = getDateTimeRange(startDate, endDate)

        // 나의 모든 모임 일정 목록을 월단위로 시작 날짜 기준으로 오름차순 정렬하여 조회
        return scheduleRepository
            .findMonthlySchedulesByMemberId(memberId, startDateTime, endDateTime)
            .map { ScheduleWithClubDto.from(it) }
    }

    /**
     * 일정 엔티티 조회
     * @param scheduleId
     * @return schedule
     */
    @Transactional(readOnly = true)
    fun getScheduleEntityById(scheduleId: Long): Schedule {
        return scheduleRepository
            .findByIdOrNull(scheduleId)
            ?: throw ServiceException(ScheduleErrorCode.SCHEDULE_NOT_FOUND)
    }

    /**
     * 일정 조회
     * @param scheduleId
     * @return scheduleDetailDto
     */
    @Transactional(readOnly = true)
    fun getScheduleById(scheduleId: Long): ScheduleDetailDto {
        val schedule = getScheduleEntityById(scheduleId)
        return ScheduleDetailDto.from(schedule)
    }

    /**
     * 활성화된 일정 엔티티 조회
     * @param scheduleId
     * @return Schedule
     */
    @Transactional(readOnly = true)
    fun getActiveScheduleEntityById(scheduleId: Long): Schedule {
        return scheduleRepository
            .findActiveScheduleById(scheduleId)
            ?: throw ServiceException(ScheduleErrorCode.SCHEDULE_NOT_FOUND)
    }

    /**
     * 활성화된 일정 조회
     * @param scheduleId
     * @return ScheduleDetailDto
     */
    @Transactional(readOnly = true)
    fun getActiveScheduleById(scheduleId: Long): ScheduleDetailDto {
        val schedule = getActiveScheduleEntityById(scheduleId)
        return ScheduleDetailDto.from(schedule)
    }

    /**
     * 특정 모임의 최신 일정 조회
     * @param clubId
     * @return schedule
     */
    @Transactional(readOnly = true)
    fun getLatestClubSchedule(clubId: Long): ScheduleDetailDto {
        val schedule = scheduleRepository
            .findFirstByClubIdOrderByIdDesc(clubId)
            ?: throw ServiceException(ScheduleErrorCode.SCHEDULE_NOT_FOUND)

        return ScheduleDetailDto.from(schedule)
    }

    /**
     * 특정 모임의 일정 개수 조회
     * @param clubId
     * @return
     */
    @Transactional(readOnly = true)
    fun countClubSchedules(clubId: Long): Long {
        return scheduleRepository.countByClubId(clubId)
    }

    /**
     * 일정 생성
     * @param reqBody (ScheduleCreateReqBody)
     * @return schedule
     */
    @Transactional
    fun createSchedule(reqBody: ScheduleCreateReqBody): ScheduleDetailDto {
        val club = getClubOrThrow(reqBody.clubId)

        // 날짜 유효성 검증
        validateDate(reqBody.startDate, reqBody.endDate)

        // 일정 생성
        val schedule = Schedule(
            reqBody.title,
            reqBody.content,
            reqBody.startDate,
            reqBody.endDate,
            reqBody.spot,
            club
        )
        scheduleRepository.save(schedule)

        return ScheduleDetailDto.from(schedule)
    }

    /**
     * 일정 수정
     * @param schedule
     * @param reqBody
     */
    @Transactional
    fun modifySchedule(schedule: Schedule, reqBody: ScheduleUpdateReqBody) {
        // 날짜 유효성 검증
        validateDate(reqBody.startDate, reqBody.endDate)

        // 일정 수정
        schedule.modify(
            reqBody.title,
            reqBody.content,
            reqBody.startDate,
            reqBody.endDate,
            reqBody.spot
        )
    }

    /**
     * 일정 삭제
     * @param schedule
     */
    @Transactional
    fun deleteSchedule(schedule: Schedule) {
        if (schedule.canDelete()) {
            // 일정 삭제 - 체크리스트 없을 시
            scheduleRepository.delete(schedule)
        } else {
            // 일정 비활성화 - 체크리스트 있을 시
            schedule.deactivate()

            // 체크리스트 비활성화
            schedule.checkList?.deactivate()
        }
    }

    /**
     * 날짜 범위 생성
     * @param startDate
     * @param endDate
     * @return
     */
    private fun getDateTimeRange(startDate: LocalDate?, endDate: LocalDate?): DateTimeRange {
        val (startDateTime, endDateTime) = when {
            // 1. 시작일과 종료일이 모두 있는 경우 (종료일 미포함)
            startDate != null && endDate != null -> {
                startDate.atStartOfDay() to endDate.atStartOfDay()
            }
            // 2. 시작일만 있는 경우, 해당 월 전체를 범위로 설정
            startDate != null -> {
                val month = YearMonth.from(startDate)
                startDate.atStartOfDay() to month.plusMonths(1).atDay(1).atStartOfDay()
            }
            // 3. 날짜 파라미터 없는 경우, 현재 달을 기준으로 설정
            else -> {
                val currentMonth = YearMonth.now()
                currentMonth.atDay(1).atStartOfDay() to currentMonth.plusMonths(1).atDay(1).atStartOfDay()
            }
        }
        validateDate(startDateTime, endDateTime)
        return DateTimeRange(startDateTime, endDateTime)
    }

    /**
     * 모임 조회
     * @param clubId
     * @return club
     */
    private fun getClubOrThrow(clubId: Long): Club {
        return clubRepository
            .findByIdOrNull(clubId)
            ?: throw ServiceException(ClubErrorCode.CLUB_NOT_FOUND)
    }

    /**
     * 날짜 유효성 검증
     * @param startDate
     * @param endDate
     */
    private fun validateDate(startDate: LocalDateTime, endDate: LocalDateTime) {
        if (startDate.isAfter(endDate)) {
            throw ServiceException(400, "시작일은 종료일보다 이전이어야 합니다.")
        }
    }
}
