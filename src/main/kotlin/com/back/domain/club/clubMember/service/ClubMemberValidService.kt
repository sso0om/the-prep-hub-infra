package com.back.domain.club.clubMember.service

import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.service.MemberService
import com.back.global.enums.ClubMemberRole
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 클럽 멤버의 유효성을 검사하는 서비스입니다.
 * 클럽 멤버의 역할을 확인하거나 클럽 멤버 여부를 확인하는 기능을 제공합니다.
 */
@Service
class ClubMemberValidService(
    private val clubMemberRepository: ClubMemberRepository,
    private val clubRepository: ClubRepository,
    private val memberService: MemberService
) {

    /**
     * 클럽 멤버의 역할을 확인합니다.
     * @param clubId 클럽 ID
     * @param memberId 멤버 ID
     * @param roles 요청된 역할 배열
     * @return 요청된 역할 중 하나라도 일치하면 true, 아니면 false
     */
    @Transactional(readOnly = true)
    fun checkMemberRole(clubId: Long, memberId: Long, roles: Array<ClubMemberRole>): Boolean {
        val club = clubRepository.findById(clubId)
            .orElseThrow{ServiceException(404, "클럽이 존재하지 않습니다.") }
        val member = memberService.findMemberById(memberId)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")

        val clubMember = clubMemberRepository.findByClubAndMember(club, member)
            ?: throw ServiceException(404, "클럽 멤버가 존재하지 않습니다.")

        // 요청된 역할이 클럽 멤버의 역할 중 하나인지 확인
        for (role in roles) {
            if (clubMember.role == role) {
                return true // 역할이 일치하면 true 반환
            }
        }
        return false // 일치하는 역할이 없으면 false 반환
    }

    /**
     * 클럽 멤버인지 확인합니다.
     * @param clubId 클럽 ID
     * @param memberId 멤버 ID
     * @return 클럽 멤버 여부
     */
    @Transactional(readOnly = true)
    fun isClubMember(clubId: Long, memberId: Long): Boolean {
        val club = clubRepository.findById(clubId)
            .orElseThrow{ServiceException(404, "클럽이 존재하지 않습니다.") }
        val member = memberService.findMemberById(memberId)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")

        return clubMemberRepository.existsByClubAndMember(club, member)
    }
}
