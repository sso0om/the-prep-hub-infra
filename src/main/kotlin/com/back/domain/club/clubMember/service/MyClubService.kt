package com.back.domain.club.clubMember.service

import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.service.ClubService
import com.back.domain.club.clubMember.dtos.ClubListItem
import com.back.domain.club.clubMember.dtos.MyClubList
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.service.MemberService
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MyClubService(
    private val clubService: ClubService,
    private val clubMemberRepository: ClubMemberRepository,
    private val memberService: MemberService,
    private val rq: Rq
) {

    /**
     * 클럽 초대를 수락하거나 거절하는 메서드
     * @param clubId 클럽 ID
     * @param accept 초대 수락 여부 (true면 수락, false면 거절)
     * @return 클럽 정보
     */
    @Transactional
    fun handleClubInvitation(clubId: Long, accept: Boolean): Club {
        // 멤버 가져오기
        val user = rq.actor ?: throw ServiceException(401, "로그인이 필요합니다.")
        // 클럽 ID로 클럽 가져오기
        val club = clubService.getClubById(clubId)
        val clubMember = clubMemberRepository.findByClubAndMember(club, user)
            ?: throw ServiceException(400, "클럽 초대 상태가 아닙니다.")

        // 클럽 멤버 상태 확인
        if (clubMember.state == ClubMemberState.JOINING)  // 가입 중인 경우
            throw ServiceException(400, "이미 가입 상태입니다.")
        else if (clubMember.state != ClubMemberState.INVITED)  // 초대 상태가 아닌 경우 (가입 신청, 탈퇴)
            throw ServiceException(400, "클럽 초대 상태가 아닙니다.")

        // 클럽 멤버 상태 업데이트
        if (accept) {
            clubMember.updateState(ClubMemberState.JOINING) // 초대 수락
        } else {
            club.clubMembers.remove(clubMember) // 클럽에서 멤버 제거
            clubMemberRepository.delete(clubMember) // 초대 거절
        }

        return club // 클럽 반환
    }

    /**
     * 클럽 가입 신청 메서드
     * @param clubId 클럽 ID
     * @return 클럽 정보
     */
    @Transactional
    fun applyForClub(clubId: Long): Club {
        // 멤버 가져오기
        val user = memberService.findMemberById(rq.actor!!.id!!)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")

        // 클럽 ID로 클럽 가져오기
        val club = clubService.getClubById(clubId)

        // 클럽이 비공개 상태인지 확인
        if (!club.isPublic) {
            throw ServiceException(403, "비공개 클럽입니다. 가입 신청이 불가능합니다.")
        }

        // 클럽 멤버 상태 확인
        if (clubMemberRepository.existsByClubAndMember(club, user)) {
            val existingMember = clubMemberRepository.findByClubAndMember(club, user)
            if (existingMember == null) throw ServiceException(404, "클럽 멤버가 존재하지 않습니다.")

            if (existingMember.state == ClubMemberState.JOINING) throw ServiceException(400, "이미 가입 상태입니다.")
            else if (existingMember.state == ClubMemberState.APPLYING) throw ServiceException(400, "이미 가입 신청 상태입니다.")
            else if (existingMember.state == ClubMemberState.INVITED) throw ServiceException(
                400,
                "클럽 초대 상태입니다. 초대를 수락해주세요."
            )
            else if (existingMember.state == ClubMemberState.WITHDRAWN) {
                // 탈퇴 상태면, 기존 클럽 멤버를 가져와서 APPLYING 상태로 변경
                existingMember.updateState(ClubMemberState.APPLYING) // 상태를 APPLYING으로 변경
                clubMemberRepository.save<ClubMember?>(existingMember) // 클럽 멤버 저장
                return club // 클럽 반환
            }
        }

        // 클럽 멤버 생성 및 저장
        val clubMember = ClubMember(
            user,
            ClubMemberRole.PARTICIPANT,  // 기본 역할은 PARTICIPANT
            ClubMemberState.APPLYING // 가입 신청 상태로 설정
        )
        club.addClubMember(clubMember) // 클럽에 멤버 추가
        clubMemberRepository.save<ClubMember?>(clubMember) // 클럽 멤버 저장

        return club // 클럽 반환
    }

    /**
     * 현재 로그인한 멤버의 클럽 정보 조회 메서드
     * @param clubId 클럽 ID
     * @return 클럽 멤버 정보
     */
    @Transactional(readOnly = true)
    fun getMyClubInfo(clubId: Long): ClubMember {
        // 현재 로그인한 멤버 가져오기
        val user = memberService.findMemberById(rq.actor!!.id!!)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")

        // 클럽 ID로 클럽 가져오기
        val club = clubService.getClubById(clubId)

        // 클럽 멤버 정보 조회
        val clubMember = clubMemberRepository.findByClubAndMember(club, user)
            ?: throw ServiceException(404, "클럽 멤버 정보가 존재하지 않습니다.")

        return clubMember
    }

    fun getMyClubs(): MyClubList {
        // 현재 로그인한 멤버 가져오기
        val user = memberService.findMemberById(rq.actor!!.id!!)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")

        // 멤버가 속한 클럽 멤버 정보 조회
        val clubMembers = clubMemberRepository.findAllByMember(user)

        // 클럽 멤버 정보를 기반으로 클럽 목록 생성
        val clubListItems = clubMembers.map { clubMember ->
            val club = clubMember.club!!
            ClubListItem(
                club.id!!,
                club.name,
                club.bio,
                club.category,
                club.imageUrl,
                club.mainSpot,
                club.eventType,
                club.startDate!!,
                club.endDate!!,
                club.isPublic,
                clubMember.role,
                clubMember.state
            )
        }.toMutableList()

        // 클럽 목록을 MyClubList DTO로 반환
        return MyClubList(clubListItems)
    }


    fun cancelClubApplication(clubId: Long): Club {
        // 현재 로그인한 멤버 가져오기
        val user = memberService.findMemberById(rq.actor!!.id!!)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")

        // 클럽 ID로 클럽 가져오기
        val club = clubService.getClubById(clubId)

        // 클럽 멤버 정보 조회
        val clubMember = clubMemberRepository.findByClubAndMember(club, user)
            ?: throw ServiceException(404, "클럽 멤버 정보가 존재하지 않습니다.")

        // 클럽 멤버 상태 확인
        if (clubMember.state != ClubMemberState.APPLYING) {
            throw ServiceException(400, "가입 신청 상태가 아닙니다.")
        }

        // 클럽에서 멤버 제거 및 클럽 멤버 삭제
        club.removeClubMember(clubMember)
        clubMemberRepository.delete(clubMember)

        return club // 클럽 반환
    }
}
