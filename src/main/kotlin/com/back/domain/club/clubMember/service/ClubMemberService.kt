package com.back.domain.club.clubMember.service

import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.service.ClubService
import com.back.domain.club.clubMember.dtos.ClubMemberInfo
import com.back.domain.club.clubMember.dtos.ClubMemberRegisterInfo
import com.back.domain.club.clubMember.dtos.ClubMemberRegisterRequest
import com.back.domain.club.clubMember.dtos.ClubMemberResponse
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.service.MemberService
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.exception.ServiceException
import com.back.global.rq.Rq
import jakarta.validation.constraints.NotBlank
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import java.util.function.Consumer

@Service
class ClubMemberService (
    private val clubMemberRepository: ClubMemberRepository,
    private val clubService: ClubService,
    private val memberService: MemberService,
    private val clubMemberValidService: ClubMemberValidService,
    private val rq: Rq
){

    /**
     * 클럽에 멤버를 추가합니다. (테스트용, controller에선 사용하지 않음)
     * @param clubId 클럽 ID
     * @param member 추가할 멤버
     * @param role 클럽 멤버 역할
     */
    @Transactional
    fun addMemberToClub(clubId: Long, member: Member, role: ClubMemberRole): ClubMember {
        val club = clubService.getClubById(clubId)

        val clubMember = ClubMember(
            member,
            role,
            ClubMemberState.INVITED
        )

        club.addClubMember(clubMember)

        return clubMemberRepository.save<ClubMember>(clubMember)
    }

    /**
     * 클럽에 멤버를 추가합니다. 요청된 이메일을 기반으로 중복된 멤버는 제외하고 추가합니다.
     * @param clubId 클럽 ID
     * @param reqBody 클럽 멤버 등록 요청 DTO
     */
    @Transactional
    fun addMembersToClub(clubId: Long, reqBody: ClubMemberRegisterRequest) {
        val club = clubService.getClubById(clubId)

        // 1. 요청 데이터에서 이메일 기준 중복 제거 (나중에 들어온 정보가 우선)
        val uniqueMemberInfoByEmail = reqBody.members
            .associateBy { it.email }

        // 2. 요청된 이메일 목록을 한 번에 조회하여 Map으로 변환 (효율적인 탐색을 위해)
        val requestEmails = uniqueMemberInfoByEmail.keys.toList()
        val existingMembersByEmail = clubMemberRepository
            .findClubMembersByClubIdAndEmails(clubId, requestEmails as MutableList<String>)
            .associateBy { it.member.getMemberInfo()?.email }

        // 3. 신규 추가/상태 변경할 멤버 목록 준비
        val membersToSave = mutableListOf<ClubMember>()
        val newMemberRequests = mutableListOf<ClubMemberRegisterInfo>()

        uniqueMemberInfoByEmail.values.forEach(Consumer { memberInfo: ClubMemberRegisterInfo? ->
            val existingMember = existingMembersByEmail[memberInfo!!.email]
            if (existingMember != null) {
                if (existingMember.state == ClubMemberState.WITHDRAWN) {
                    existingMember.updateState(ClubMemberState.INVITED)
                    // 요청된 역할로 업데이트
                    existingMember.updateRole(ClubMemberRole.fromString(memberInfo.role.uppercase(Locale.getDefault())))
                    membersToSave.add(existingMember)
                }
            } else {
                newMemberRequests.add(memberInfo)
            }
        })

        // 4. 정원 초과 여부 검사 (효율적인 COUNT 쿼리 사용)
        val currentActiveMembers = clubMemberRepository.countActiveMembersByClubId(clubId)
        if (currentActiveMembers + newMemberRequests.size > club.maximumCapacity) {
            throw ServiceException(400, "클럽의 최대 멤버 수를 초과했습니다.")
        }

        // 5. 새로운 멤버 엔티티 생성
        for (memberInfo in newMemberRequests) {
            val member = memberService.findMemberByEmail(memberInfo.email)
            val newClubMember = ClubMember(
                member,
                ClubMemberRole.fromString(memberInfo.role.uppercase(Locale.getDefault())),
                ClubMemberState.INVITED
            )

            club.addClubMember(newClubMember) // 양방향 연관관계 설정
            membersToSave.add(newClubMember)
        }

        // 6. 변경/추가된 모든 멤버 정보를 한 번에 저장 (Batch Insert/Update)
        if (membersToSave.isNotEmpty()) {
            clubMemberRepository.saveAll(membersToSave)
        }
    }

    /**
     * 클럽에서 멤버를 탈퇴시킵니다.
     * @param clubId 클럽 ID
     * @param memberId 탈퇴할 멤버 ID
     */
    @Transactional
    fun withdrawMemberFromClub(clubId: Long, memberId: Long) {
        val user = rq.actor ?: throw ServiceException(401, "로그인이 필요합니다.")
        val club = clubService.getClubById(clubId)
        val member = memberService.findMemberById(memberId)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")
        val clubMember = clubMemberRepository.findByClubAndMember(club, member)
            ?: throw ServiceException(404, "클럽 멤버가 존재하지 않습니다.")

        // 호스트 본인이 탈퇴하려는 경우 예외 처리
        if (user.id == memberId && clubMember.role == ClubMemberRole.HOST) {
            throw ServiceException(400, "호스트는 탈퇴할 수 없습니다.")
        }

        // 클럽에서 멤버 탈퇴
        clubMember.updateState(ClubMemberState.WITHDRAWN)
        clubMemberRepository.save<ClubMember?>(clubMember)
    }

    /**
     * 클럽 멤버의 역할을 변경합니다.
     * @param clubId 클럽 ID
     * @param memberId 멤버 ID
     * @param role 변경할 역할
     */
    @Transactional
    fun changeMemberRole(clubId: Long, memberId: Long, role: @NotBlank String) {
        val club = clubService.getClubById(clubId)
        val member = memberService.findMemberById(memberId)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")
        val clubMember = clubMemberRepository.findByClubAndMember(club, member)
            ?: throw ServiceException(404, "클럽 멤버가 존재하지 않습니다.")

        // 호스트 본인이 역할을 변경하려는 경우 예외 처리
        if (member.id == rq.actor?.id) {
            throw ServiceException(400, "호스트는 본인의 역할을 변경할 수 없습니다.")
        }

        // 호스트 권한 부여 금지
        if (role.equals(ClubMemberRole.HOST.name, ignoreCase = true)) {
            throw ServiceException(400, "호스트 권한은 직접 부여할 수 없습니다.")
        }

        // 역할 변경
        clubMember.updateRole(ClubMemberRole.fromString(role.uppercase(Locale.getDefault())))
        clubMemberRepository.save<ClubMember?>(clubMember)
    }

    /**
     * 클럽의 멤버 목록을 조회합니다.
     * @param clubId 클럽 ID
     * @param state 상태 필터링 (선택적)
     * @return 클럽 멤버 목록 DTO
     */
    @Transactional(readOnly = true)
    fun getClubMembers(clubId: Long, state: String?): ClubMemberResponse {
        // 클럽 확인
        val club = clubService.getClubById(clubId)

        // 클럽멤버 목록 반환
        val clubMembers = if (state != null) {
            clubMemberRepository.findByClubAndState(club, ClubMemberState.fromString(state))
        } else {
            clubMemberRepository.findByClub(club)
        }

        // 클럽 멤버 정보를 DTO로 변환
        val memberInfos = clubMembers
            .filter { it.state != ClubMemberState.WITHDRAWN }
            .map { cm ->
                val m = cm.member
                ClubMemberInfo(
                    clubMemberId = cm.id!!,
                    memberId = m.id!!,
                    nickname = m.nickname,
                    tag = m.tag!!,
                    role = cm.role,
                    email = m.getMemberInfo()?.email ?: "",
                    memberType = m.memberType,
                    profileImageUrl = m.getMemberInfo()?.profileImageUrl ?: "",
                    state = cm.state
                )
            }
            .toMutableList()

        return ClubMemberResponse(memberInfos)
    }

    /**
     * 클럽과 멤버로 가입 완료한 클럽 멤버 조회
     * @param club 모임 엔티티
     * @param member 멤버 엔티티
     * @return 클럽 멤버 엔티티
     */
    fun getClubMember(club: Club, member: Member): ClubMember {
        return clubMemberRepository.findByClubAndMemberAndState(club, member, ClubMemberState.JOINING)
            ?: throw AccessDeniedException("권한이 없습니다.")
    }

    /**
     * 클럽과 멤버로 클럽 멤버 존재 여부 확인
     * @param club 모임 엔티티
     * @param member 멤버 엔티티
     * @return 클럽 멤버 존재 여부
     */
    fun existsByClubAndMember(club: Club, member: Member): Boolean {
        return clubMemberRepository
            .existsByClubAndMemberAndState(club, member, ClubMemberState.JOINING)
    }

    /**
     * 클럽 가입 신청을 승인하거나 거절합니다.
     * @param clubId 클럽 ID
     * @param memberId 멤버 ID
     * @param approve true면 승인, false면 거절
     */
    @Transactional
    fun handleMemberApplication(clubId: Long, memberId: Long, approve: Boolean) {
        val club = clubService.getClubById(clubId)
        val member = memberService.findMemberById(memberId)
            ?: throw ServiceException(404, "멤버가 존재하지 않습니다.")
        val clubMember = clubMemberRepository.findByClubAndMember(club, member)
            ?: throw ServiceException(400, "가입 신청 상태가 아닙니다.")

        // 현재 상태가 APPLYING이 아닌 경우 예외 처리
        if (clubMember.state != ClubMemberState.APPLYING) {
            if (clubMember.state == ClubMemberState.JOINING) throw ServiceException(400, "이미 가입 상태입니다.")
            else throw ServiceException(400, "가입 신청 상태가 아닙니다.")
        }

        // 승인 또는 거절 처리
        if (approve) {
            clubMember.updateState(ClubMemberState.JOINING)
            clubMemberRepository.save<ClubMember?>(clubMember)
        } else {
            clubMemberRepository.delete(clubMember)
            // 거절 시 클럽에서 멤버 제거
            club.removeClubMember(clubMember)
        }
    }
}
