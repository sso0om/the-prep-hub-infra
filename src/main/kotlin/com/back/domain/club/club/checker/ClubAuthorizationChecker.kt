package com.back.domain.club.club.checker

import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.club.service.ClubService
import com.back.domain.club.clubMember.service.ClubMemberService
import com.back.domain.member.member.service.MemberService
import com.back.global.enums.ClubMemberRole
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component("clubAuthorizationChecker")
class ClubAuthorizationChecker (
    private val memberService: MemberService,
    private val clubRepository: ClubRepository,
    private val clubMemberService: ClubMemberService,
    private val clubService: ClubService
){

    /**
     * 모임이 존재하는지 확인
     * @param clubId 모임 ID
     * @return 모임 존재 여부
     */
    @Transactional(readOnly = true)
    fun isClubExists(clubId: Long): Boolean {
        return clubRepository.existsById(clubId)
    }

    /**
     * 모임 호스트 권한이 있는지 확인
     * @param clubId 모임 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 호스트 권한 여부
     */
    @Transactional(readOnly = true)
    fun isClubHost(clubId: Long, memberId: Long): Boolean {
        val club = clubService.getClubById(clubId)

        return club.leaderId == memberId
    }

    /**
     * 모임 호스트 권한이 있는지 확인 (종료 안된 활성화된 모임에 대해서만)
     * @param clubId 모임 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 호스트 권한 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubHost(clubId: Long, memberId: Long): Boolean {
        val club = clubService.getValidAndActiveClub(clubId)

        return club.leaderId == memberId
    }

    /**
     * 모임 매니저의 역할 확인 (종료 안된 활성화된 모임에 대해서만)
     * @param clubId 모임 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 매니저 권한 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubManager(clubId: Long, memberId: Long): Boolean {
        val club = clubService.getValidAndActiveClub(clubId)
        val member = memberService.getMember(memberId)
        val clubMember = clubMemberService.getClubMember(club, member)

        return clubMember.role == ClubMemberRole.MANAGER
    }

    /**
     * 모임 호스트 또는 매니저 권한 확인 (종료 안된 활성화된 모임에 대해서만)
     * @param clubId 모임 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 호스트 또는 매니저 권한 여부
     */
    @Transactional(readOnly = true)
    fun isActiveClubManagerOrHost(clubId: Long, memberId: Long): Boolean {
        val club = clubService.getValidAndActiveClub(clubId)
        val member = memberService.getMember(memberId)
        val clubMember = clubMemberService.getClubMember(club, member)

        return clubMember.role == ClubMemberRole.MANAGER
                || club.leaderId == memberId
    }

    /**
     * 가입된(JOINING) 모임 참여자 여부 확인 (활성화된 모임에 대해서만)
     * @param clubId 모임 ID
     * @param memberId 로그인 유저 ID
     * @return 모임 참여자 여부
     */
    @Transactional(readOnly = true)
    fun isClubMember(clubId: Long, memberId: Long): Boolean {
        val club = clubService.getActiveClub(clubId)
        val member = memberService.getMember(memberId)

        return clubMemberService.existsByClubAndMember(club, member)
    }

    /**
     * 로그인 유저가 요청한 멤버 ID와 일치하는지 확인
     * @param targetMemberId 멤버 ID
     * @param currentUserId 로그인 유저 ID
     * @return 로그인 유저 - 요청한 멤버 ID 일치 여부
     */
    @Transactional(readOnly = true)
    fun isSelf(targetMemberId: Long, currentUserId: Long): Boolean {
        return targetMemberId == currentUserId
    }
}
