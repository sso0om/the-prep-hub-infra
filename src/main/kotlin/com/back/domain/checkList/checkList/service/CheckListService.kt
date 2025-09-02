package com.back.domain.checkList.checkList.service

import com.back.domain.checkList.checkList.dto.CheckListDto
import com.back.domain.checkList.checkList.dto.CheckListUpdateReqDto
import com.back.domain.checkList.checkList.dto.CheckListWriteReqDto
import com.back.domain.checkList.checkList.entity.CheckList
import com.back.domain.checkList.checkList.entity.CheckListItem
import com.back.domain.checkList.checkList.repository.CheckListRepository
import com.back.domain.checkList.itemAssign.entity.ItemAssign
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.schedule.schedule.repository.ScheduleRepository
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.rq.Rq
import com.back.global.rsData.RsData
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true) // 1. 생성자 주입과 읽기 전용 트랜잭션 설정
class CheckListService(
    private val checkListRepository: CheckListRepository,
    private val scheduleRepository: ScheduleRepository,
    private val clubRepository: ClubRepository,
    private val rq: Rq
) {
    // 체커에 사용되는 메서드
    fun getActiveCheckListById(checkListId: Long): CheckList {
        // 활성화된 체크리스트 조회
        return checkListRepository
            .findActiveCheckListById(checkListId)
            .orElseThrow { NoSuchElementException("체크리스트를 찾을 수 없습니다") }
    }

    @Transactional
    fun write(checkListWriteReqDto: CheckListWriteReqDto): RsData<CheckListDto> {
        // 2. 엘비스 연산자(?:)를 사용한 간결한 null 처리
        val member = rq.actor ?: return RsData.of(404, "멤버를 찾을 수 없습니다")

        val schedule = scheduleRepository.findById(checkListWriteReqDto.scheduleId)
            .orElse(null) ?: return RsData.of(404, "일정을 찾을 수 없습니다")

        if (schedule.checkList != null) {
            return RsData.of(409, "이미 체크리스트가 존재합니다")
        }

        // 3. find 확장 함수로 클럽 멤버 조회 간소화
        val clubMember = schedule.club.clubMembers.find { it.member.id == member.id }
            ?: return RsData.of(403, "클럽 멤버가 아닙니다")

        if (clubMember.state != ClubMemberState.JOINING) {
            return RsData.of(403, "클럽에 가입된 상태가 아닙니다")
        }

        if (clubMember.role == ClubMemberRole.PARTICIPANT) {
            return RsData.of(403, "호스트 또는 관리자만 체크리스트를 생성할 수 있습니다")
        }

        val checkListItems = try {
            // 4. map, associateBy 등 컬렉션 함수를 활용한 데이터 처리
            val clubMembersById = schedule.club.clubMembers.associateBy { it.id }
            checkListWriteReqDto.checkListItems.map { req ->
                val itemAssigns = (req.itemAssigns ?: emptyList()).map { itemAssignReq ->
                    val assignedClubMember = clubMembersById[itemAssignReq.clubMemberId]
                        ?: throw IllegalArgumentException("클럽 멤버를 찾을 수 없습니다")
                    ItemAssign(clubMember = assignedClubMember)
                }
                // 5. Lombok Builder 대신 주 생성자 사용
                CheckListItem(
                    content = req.content,
                    category = req.category,
                    sequence = req.sequence,
                    itemAssigns = itemAssigns
                )
            }
        } catch (e: IllegalArgumentException) {
            return RsData.of(403, e.message)
        }

        val newCheckList = CheckList(
            isActive = true,
            schedule = schedule,
            checkListItems = checkListItems
        )

        val savedCheckList = checkListRepository.save(newCheckList)

        return RsData.of(201, "체크리스트 생성 성공", CheckListDto(savedCheckList))
    }

    fun getCheckList(checkListId: Long): RsData<CheckListDto> {
        val member = rq.actor ?: return RsData.of(404, "멤버를 찾을 수 없습니다")

        val checkList = checkListRepository.findById(checkListId)
            .orElse(null) ?: return RsData.of(404, "체크리스트를 찾을 수 없습니다")

        val clubMember = checkList.schedule.club.clubMembers.find { it.member.id == member.id }
            ?: return RsData.of(403, "클럽 멤버가 아닙니다")

        if (clubMember.state != ClubMemberState.JOINING) {
            return RsData.of(403, "클럽에 가입된 상태가 아닙니다")
        }

        return RsData.of(200, "체크리스트 조회 성공", CheckListDto(checkList))
    }

    @Transactional
    fun updateCheckList(checkListId: Long, checkListUpdateReqDto: CheckListUpdateReqDto): RsData<CheckListDto> {
        val member = rq.actor ?: return RsData.of(404, "멤버를 찾을 수 없습니다")

        val checkList = checkListRepository.findById(checkListId)
            .orElse(null) ?: return RsData.of(404, "체크리스트를 찾을 수 없습니다")

        val clubMember = checkList.schedule.club.clubMembers.find { it.member.id == member.id }
            ?: return RsData.of(403, "클럽 멤버가 아닙니다")

        if (clubMember.state != ClubMemberState.JOINING) {
            return RsData.of(403, "클럽에 가입된 상태가 아닙니다")
        }

        if (clubMember.role == ClubMemberRole.PARTICIPANT) {
            return RsData.of(403, "호스트 또는 관리자만 체크리스트를 수정할 수 있습니다")
        }

        val newCheckListItems = try {
            val clubMembersById = checkList.schedule.club.clubMembers.associateBy { it.id }
            checkListUpdateReqDto.checkListItems.map { req ->
                val itemAssigns = (req.itemAssigns ?: emptyList()).map { itemAssignReq ->
                    val assignedClubMember = clubMembersById[itemAssignReq.clubMemberId]
                        ?: throw IllegalArgumentException("클럽 멤버를 찾을 수 없습니다")
                    ItemAssign(
                        clubMember = assignedClubMember,
                        isChecked = itemAssignReq.isChecked
                    )
                }
                CheckListItem(
                    content = req.content,
                    category = req.category,
                    sequence = req.sequence,
                    isChecked = req.isChecked,
                    itemAssigns = itemAssigns
                )
            }
        } catch (e: IllegalArgumentException) {
            return RsData.of(403, e.message)
        }

        checkList.updateCheckListItems(newCheckListItems)

        val updatedCheckList = checkListRepository.save(checkList)
        return RsData.of(200, "체크리스트 수정 성공", CheckListDto(updatedCheckList))
    }

    @Transactional
    fun deleteCheckList(checkListId: Long): RsData<Void> { // 6. 명확한 반환 타입
        val member = rq.actor ?: return RsData.of(404, "멤버를 찾을 수 없습니다")

        val checkList = checkListRepository.findById(checkListId)
            .orElse(null) ?: return RsData.of(404, "체크리스트를 찾을 수 없습니다")

        val clubMember = checkList.schedule.club.clubMembers.find { it.member.id == member.id }
            ?: return RsData.of(403, "클럽 멤버가 아닙니다")

        if (clubMember.state != ClubMemberState.JOINING) {
            return RsData.of(403, "클럽에 가입된 상태가 아닙니다")
        }

        if (clubMember.role == ClubMemberRole.PARTICIPANT) {
            return RsData.of(403, "호스트 또는 관리자만 체크리스트를 삭제할 수 있습니다")
        }

        checkListRepository.delete(checkList)

        return RsData.of(200, "체크리스트 삭제 성공")
    }

    fun getCheckListByGroupId(groupId: Long): RsData<List<CheckListDto>> {
        val member = rq.actor ?: return RsData.of(404, "멤버를 찾을 수 없습니다")

        val club = clubRepository.findById(groupId)
            .orElse(null) ?: return RsData.of(404, "클럽을 찾을 수 없습니다")

        val clubMember = club.clubMembers.find { it.member.id == member.id }
            ?: return RsData.of(403, "클럽 멤버가 아닙니다")

        if (clubMember.state != ClubMemberState.JOINING) {
            return RsData.of(403, "클럽에 가입된 상태가 아닙니다")
        }

        val checkListDtos = club.clubSchedules
            .mapNotNull { it.checkList } // 7. mapNotNull로 null 안전하게 처리
            .filter { it.isActive }
            .map { CheckListDto(it) }

        return RsData.of(200, "체크리스트 목록 조회 성공", checkListDtos)
    }
}