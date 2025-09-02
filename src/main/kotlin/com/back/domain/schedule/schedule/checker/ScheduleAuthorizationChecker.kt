package com.back.domain.schedule.schedule.checker

import com.back.domain.club.club.checker.ClubAuthorizationChecker
import com.back.domain.schedule.schedule.service.ScheduleService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component("scheduleAuthorizationChecker")
class ScheduleAuthorizationChecker(
    private val scheduleService: ScheduleService,
    private val clubChecker: ClubAuthorizationChecker,
) {
    /**
     * 모임의 활성화된 스케줄에 대해 로그인한 유저가 모임 호스트인지 확인
     * @param scheduleId
     * @param memberId
     * @return 모임 호스트 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubHost(scheduleId: Long, memberId: Long): Boolean {
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)
        val clubId = schedule.club.id !!

        return clubChecker.isActiveClubHost(clubId, memberId)
    }

    /**
     * 모임의 활성화된 스케줄에 대해 로그인한 유저가 모임 관리자 또는 호스트인지 확인
     * @param scheduleId
     * @param memberId
     * @return 모임 관리자 또는 호스트 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubManagerOrHost(scheduleId: Long, memberId: Long): Boolean {
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)
        val clubId = schedule.club.id !!

        return clubChecker.isActiveClubManagerOrHost(clubId, memberId)
    }

    /**
     * 모임의 활성화된 스케줄에 대해 로그인한 유저가 모임 멤버인지 확인
     * @param scheduleId
     * @param memberId
     * @return 모임 멤버 여부
     */
    @Transactional(readOnly = true)
    fun isClubMember(scheduleId: Long, memberId: Long): Boolean {
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)
        val clubId = schedule.club.id !!

        return clubChecker.isClubMember(clubId, memberId)
    }
}
