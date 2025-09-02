package com.back.domain.club.clubMember.repository

import com.back.domain.club.club.entity.Club
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.member.member.entity.Member
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface ClubMemberRepository : JpaRepository<ClubMember, Long> {
    fun findAllByClubId(clubId: Long): MutableList<ClubMember>

    // 요청 이메일 목록에 해당하는 ClubMember 정보를 한 번에 조회
    @Query(
        """
       SELECT cm
       FROM ClubMember cm
       WHERE cm.club.id = :clubId
              AND cm.member.memberInfo.email
              IN :emails
    
    """
    )
    fun findClubMembersByClubIdAndEmails(
        @Param("clubId") clubId: Long,
        @Param("emails") emails: MutableList<String>
    ): MutableList<ClubMember>

    //정원 체크를 위한 현재 활동 멤버 수 조회 (탈퇴 제외)
    @Query(
        """
    SELECT COUNT(cm)
    FROM ClubMember cm
    WHERE cm.club.id = :clubId
        AND cm.state != 'WITHDRAWN'
    """
    )
    fun countActiveMembersByClubId(@Param("clubId") clubId: Long): Long

    fun findByClubAndMember(club: Club, member: Member): ClubMember?

    // 특정 모임에 특정 멤버가 특정 상태로 존재하는지 확인
    fun findByClubAndMemberAndState(
        club: Club,
        member: Member,
        clubMemberState: ClubMemberState
    ): ClubMember?

    fun findByClubAndState(club: Club, clubMemberState: ClubMemberState): MutableList<ClubMember>

    fun findByClub(club: Club): MutableList<ClubMember>

    fun existsByClubAndMember(club: Club, member: Member): Boolean

    // 특정 모임에 특정 멤버가 특정 상태로 존재하는지 확인
    fun existsByClubAndMemberAndState(club: Club, member: Member, clubMemberState: ClubMemberState): Boolean

    fun existsByClubAndMemberAndRoleIn(club: Club, member: Member, roles: MutableList<ClubMemberRole>): Boolean

    fun findAllByMember(user: Member): MutableList<ClubMember>
}
