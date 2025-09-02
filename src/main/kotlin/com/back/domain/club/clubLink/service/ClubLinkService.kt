package com.back.domain.club.clubLink.service

import com.back.domain.club.club.dtos.SimpleClubInfoResponse
import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.clubLink.dtos.CreateClubLinkResponse
import com.back.domain.club.clubLink.entity.ClubLink
import com.back.domain.club.clubLink.repository.ClubLinkRepository
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.enums.ClubApplyResult
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.exception.ServiceException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class ClubLinkService(
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubLinkRepository: ClubLinkRepository,
    private val memberRepository: MemberRepository,
    @Value("\${app.frontend.base-url}") private val frontendBaseUrl: String
) {

    @Transactional
    fun createClubLink(user: Member, clubId: Long): CreateClubLinkResponse {
        val club = isClubExist(clubId)

        // 권한 체크
        validateClubManagerOrHost(club, user)

        val now = LocalDateTime.now()

        // 기존 활성 링크 반환
        val existingLink = clubLinkRepository.findByClubAndExpiresAtAfter(club, now)
        if (existingLink != null) {
            val link = "$frontendBaseUrl/clubs/invite?token=${existingLink.inviteCode}"
            return CreateClubLinkResponse(link)
        }

        // UUID 기반 초대 코드 생성
        val inviteCode = UUID.randomUUID().toString()
        val expireAt = now.plusDays(7)

        // DB 저장
        val clubLink = ClubLink(
            id = null,
            inviteCode = inviteCode,
            createdAt = now,
            expiresAt = expireAt,
            club = club
        )
        clubLinkRepository.save(clubLink)

        val link = "$frontendBaseUrl/clubs/invite?token=$inviteCode"
        return CreateClubLinkResponse(link)
    }

    fun getExistingClubLink(user: Member, clubId: Long): CreateClubLinkResponse {
        val club = isClubExist(clubId)

        // 권한 체크
        validateClubManagerOrHost(club, user)

        val now = LocalDateTime.now()
        val existingLink = clubLinkRepository.findByClubAndExpiresAtAfter(club, now)
            ?: throw ServiceException(400, "활성화된 초대 링크를 찾을 수 없습니다.")

        val link = "$frontendBaseUrl/clubs/invite?token=${existingLink.inviteCode}"
        return CreateClubLinkResponse(link)
    }

    fun applyToPrivateClub(user: Member, token: String): ClubApplyResult {
        val clubLink = validateInviteTokenOrThrow(token)
        val club = clubLink.club

        val existingMemberOpt = clubMemberRepository.findByClubAndMember(club, user)
        existingMemberOpt?.let { member ->
            return when (member.state) {
                ClubMemberState.JOINING -> ClubApplyResult.ALREADY_JOINED
                ClubMemberState.APPLYING -> ClubApplyResult.ALREADY_APPLYING
                ClubMemberState.INVITED -> ClubApplyResult.ALREADY_INVITED
                else -> throw ServiceException(400, "해당 상태에서는 가입할 수 없습니다.")
            }
        }

        val clubMember = ClubMember(
            member = user,
            role = ClubMemberRole.PARTICIPANT,
            state = ClubMemberState.APPLYING
        ).apply { this.club = club }

        clubMemberRepository.save(clubMember)

        return ClubApplyResult.SUCCESS
    }

    fun getClubInfoByInvitationToken(token: String): SimpleClubInfoResponse {
        val clubLink = validateInviteTokenOrThrow(token)
        val club = clubLink.club

        val leader = club.leaderId?.let {
            memberRepository.findById(it).orElseThrow {
                ServiceException(400, "해당 아이디의 모임장을 찾을 수 없습니다.")
            }
        } ?: throw ServiceException(400, "모임장 ID가 없습니다.")

        return SimpleClubInfoResponse(
            clubId = club.id!!,
            name = club.name,
            category = club.category.name,
            imageUrl = club.imageUrl,
            mainSpot = club.mainSpot,
            eventType = club.eventType.name,
            startDate = club.startDate.toString(),
            endDate = club.endDate.toString(),
            leaderId = club.leaderId!!,
            leaderName = leader.nickname
        )
    }

    // =============================== 기타 메서드 ===============================

    fun isClubExist(clubId: Long): Club =
        clubRepository.findById(clubId)
            .orElseThrow{ServiceException(400, "해당 id의 클럽을 찾을 수 없습니다.")}

    fun validateClubManagerOrHost(club: Club, user: Member) {
        if (!clubMemberRepository.existsByClubAndMemberAndRoleIn(
                club,
                user,
                mutableListOf(ClubMemberRole.MANAGER, ClubMemberRole.HOST)
            )
        ) {
            throw ServiceException(400, "호스트나 매니저만 초대 링크를 관리할 수 있습니다.")
        }
    }

    fun validateInviteTokenOrThrow(token: String): ClubLink {
        val clubLink = clubLinkRepository.findByInviteCode(token)
            ?: throw ServiceException(400, "초대 토큰이 유효하지 않습니다.")

        if (clubLink.isExpired()) {
            throw ServiceException(400, "초대 토큰이 만료되었습니다.")
        }
        return clubLink
    }
}
