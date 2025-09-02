package com.back.domain.checkList.checkList.checker

import com.back.domain.checkList.checkList.service.CheckListService
import com.back.domain.club.club.checker.ClubAuthorizationChecker
import com.back.domain.club.club.entity.Club
import com.back.domain.schedule.schedule.service.ScheduleService
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.NoSuchElementException

@Component("checkListAuthorizationChecker")
class CheckListAuthorizationChecker(
    private val scheduleService: ScheduleService,
    private val checkListService: CheckListService,
    private val clubChecker: ClubAuthorizationChecker
) {

    /**
     * 체크리스트가 속한 모임의 호스트 권한이 있는지 확인
     * @param checkListId 체크리스트 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 호스트 권한 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubHost(checkListId: Long, memberId: Long): Boolean {
        val club = getClubByCheckListId(checkListId)
        return clubChecker.isActiveClubHost(club.id!!, memberId)
    }

    /**
     * 체크리스트가 속한 모임의 매니저 또는 호스트 권한이 있는지 확인
     * @param checkListId 체크리스트 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 매니저 또는 호스트 권한 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubManagerOrHost(checkListId: Long, memberId: Long): Boolean {
        val club = getClubByCheckListId(checkListId)
        return clubChecker.isActiveClubManagerOrHost(club.id!!, memberId)
    }

    /**
     * 체크리스트가 속한 모임의 참여자인지 확인
     * @param checkListId 체크리스트 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 멤버 여부
     */
    @Transactional(readOnly = true)
    fun isClubMember(checkListId: Long, memberId: Long): Boolean {
        val club = getClubByCheckListId(checkListId)
        return clubChecker.isClubMember(club.id!!, memberId)
    }

    /**
     * 모임의 활성화된 스케줄에 대해 로그인한 유저가 모임 관리자 또는 호스트인지 확인
     * @param scheduleId
     * @param memberId
     * @return 모임 관리자 또는 호스트 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubManagerOrHostByScheduleId(scheduleId: Long, memberId: Long): Boolean {
        val club = getClubByScheduleId(scheduleId)
        val clubId = club.id!!
        return clubChecker.isActiveClubManagerOrHost(clubId, memberId)
    }


    // 헬퍼 메서드 -----

    /**
     * 활성화된 체크리스트의 모임 조회
     * @param checkListId 체크리스트 ID
     * @return 모임 엔티티
     */
    private fun getClubByCheckListId(checkListId: Long): Club {
        // 활성화된 체크리스트 조회
        val checkList = checkListService.getActiveCheckListById(checkListId)

        // Schedule 엔티티 조회
        val schedule = checkList.schedule
        if (!schedule.isActive) {
            throw NoSuchElementException("일정을 찾을 수 없습니다")
        }
        // Schedule에 CheckList가 이미 존재하는 경우
        if (schedule.checkList != null && schedule.checkList != checkList) {
            throw ServiceException(409, "이미 체크리스트가 존재합니다")
        }
        // Club 엔티티 반환
        return schedule.club
    }

    private fun getClubByScheduleId(scheduleId: Long): Club {
        // Schedule 엔티티 조회
        val schedule = scheduleService.getActiveScheduleEntityById(scheduleId)

        if (!schedule.isActive) {
            throw NoSuchElementException("일정을 찾을 수 없습니다.")
        }
        // Schedule에 CheckList가 이미 존재하는 경우
        if (schedule.checkList != null) {
            throw ServiceException(409, "이미 체크리스트가 존재합니다")
        }
        // Club 엔티티 반환
        return schedule.club
    }
}