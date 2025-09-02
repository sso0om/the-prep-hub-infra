package com.back.domain.checkList

import com.back.domain.checkList.checkList.entity.CheckList
import com.back.domain.checkList.checkList.entity.CheckListItem
import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.back.domain.schedule.schedule.entity.Schedule
import com.back.domain.schedule.schedule.repository.ScheduleRepository
import com.back.global.enums.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@Transactional
class ApiV1CheckListControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var clubRepository: ClubRepository

    @Autowired
    private lateinit var scheduleRepository: ScheduleRepository

    @Autowired
    private lateinit var clubMemberRepository: ClubMemberRepository

    private lateinit var club: Club
    private lateinit var club2: Club
    private lateinit var member: Member
    private lateinit var clubMember: ClubMember
    private lateinit var clubMember2: ClubMember
    private lateinit var schedule: Schedule
    private lateinit var schedule2: Schedule

    @BeforeEach
    fun setUp() {
        member = memberRepository.findByMemberInfo_Email("hgd222@test.com")
            ?: throw IllegalStateException("테스트용 멤버(email: hgd222@test.com)가 존재하지 않습니다")

        // 참고: ClubMember, Club 엔티티는 코틀린으로 변환되었다고 가정하고 주 생성자를 사용합니다.
        clubMember = ClubMember(member, ClubMemberRole.MANAGER, ClubMemberState.JOINING)
        clubMember2 = ClubMember(member, ClubMemberRole.MANAGER, ClubMemberState.JOINING)

        val clubBuilder = Club(
            name = "테스트 클럽",
            bio = "테스트 클럽 설명",
            category = ClubCategory.CULTURE,
            mainSpot = "테스트 장소",
            maximumCapacity = 10,
            recruitingStatus = true,
            eventType = EventType.LONG_TERM,
            startDate = LocalDate.parse("2025-07-05"),
            endDate = LocalDate.parse("2026-08-30"),
            isPublic = false,
            leaderId = member.id!!,
            state = true
        )

        val clubBuilder2 = Club(
            name = "테스트 클럽2",
            bio = "테스트 클럽 설명2",
            category = ClubCategory.CULTURE,
            mainSpot = "테스트 장소2",
            maximumCapacity = 10,
            recruitingStatus = true,
            eventType = EventType.LONG_TERM,
            startDate = LocalDate.parse("2025-07-05"),
            endDate = LocalDate.parse("2026-08-30"),
            isPublic = false,
            leaderId = member.id!!,
            state = true
        )

        clubBuilder.addClubMember(clubMember)
        clubBuilder2.addClubMember(clubMember2)

        club = clubRepository.save(clubBuilder)
        club2 = clubRepository.save(clubBuilder2)

        val scheduleBuilder = Schedule(
            title = "테스트 일정",
            content = "테스트 일정 내용",
            startDate = LocalDateTime.parse("2025-08-15T10:00:00"),
            endDate = LocalDateTime.parse("2025-08-16T10:00:00"),
            spot = "테스트 장소",
            club = club
        )

        val scheduleBuilder2 = Schedule(
            title = "테스트 일정2",
            content = "테스트 일정 내용2",
            startDate = LocalDateTime.parse("2025-08-20T10:00:00"),
            endDate = LocalDateTime.parse("2025-08-21T10:00:00"),
            spot = "테스트 장소2",
            club = club
        )

        val scheduleBuilder3 = Schedule(
            title = "테스트 일정3",
            content = "테스트 일정 내용3",
            startDate = LocalDateTime.parse("2025-08-20T10:00:00"),
            endDate = LocalDateTime.parse("2025-08-21T10:00:00"),
            spot = "테스트 장소3",
            club = club2
        )

        val checkListItems = mutableListOf(
            CheckListItem(
                content = "테스트 체크리스트 아이템1",
                category = CheckListItemCategory.PREPARATION,
                sequence = 1,
                isChecked = false,
                itemAssigns = mutableListOf()
            )
        )

        val checkListBuilder = CheckList(
            isActive = true,
            schedule = scheduleBuilder2,
            checkListItems = checkListItems
        )

        scheduleBuilder2.updateCheckList(checkListBuilder)

        schedule = scheduleRepository.save(scheduleBuilder)
        schedule2 = scheduleRepository.save(scheduleBuilder2)
        scheduleRepository.save(scheduleBuilder3)
    }

    fun checkListCreate(scheduleId: Long): JsonNode {
        val requestBody = """
          {
            "scheduleId": $scheduleId,
            "checkListItems": [
              {
                "content": "체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": ${clubMember.id}
                  }
                ]
              },
              {
                "content": "체크리스트 아이템 2",
                "category": "${CheckListItemCategory.ETC.name}",
                "sequence": 2,
                "itemAssigns": []
              }
            ]
          }
        """.trimIndent()

        val result = mockMvc.perform(
            post("/api/v1/checklists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andDo(print())
            .andReturn()
        val responseContent = result.response.contentAsString
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        return objectMapper.readTree(responseContent)
    }

    @Test
    @DisplayName("체크리스트 생성")
    @WithUserDetails(value = "hgd222@test.com")
    fun t1() {
        val requestBody = """
          {
            "scheduleId": ${schedule.id},
            "checkListItems": [
              {
                "content": "체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": ${clubMember.id}
                  }
                ]
              },
              {
                "content": "체크리스트 아이템 2",
                "category": "${CheckListItemCategory.ETC.name}",
                "sequence": 2,
                "itemAssigns": []
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/checklists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value("체크리스트 생성 성공"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 생성 실패 - 일정이 존재하지 않는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t2() {
        val requestBody = """
          {
            "scheduleId": 9999,
            "checkListItems": [
              {
                "content": "체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": ${clubMember.id}
                  }
                ]
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/checklists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("일정을 찾을 수 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 생성 실패 - 클럽 멤버가 아닌 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t3() {
        val anotherMember = Member.createMember("다른 유저", "password", null)
        memberRepository.save(anotherMember)

        val requestBody = """
          {
            "scheduleId": ${schedule.id},
            "checkListItems": [
              {
                "content": "체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": 9999
                  }
                ]
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/checklists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("클럽 멤버를 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 생성 실패 - 호스트 또는 관리자만 체크리스트를 생성할 수 있는 경우")
    @WithUserDetails(value = "chs4s@test.com")
    fun t4() {
        val anotherMember = memberRepository.findById(2L).orElseThrow()
        val anotherClubMember = ClubMember(anotherMember, ClubMemberRole.PARTICIPANT, ClubMemberState.JOINING)

        club.addClubMember(anotherClubMember)
        clubRepository.save(club)

        val requestBody = """
          {
            "scheduleId": ${schedule.id},
            "checkListItems": [
              {
                "content": "체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": ${member.id}
                  }
                ]
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/checklists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 생성 실패 - 일정에 체크리스트가 이미 존재하는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t8() {
        checkListCreate(schedule.id!!)

        val requestBody = """
          {
            "scheduleId": ${schedule.id},
            "checkListItems": [
              {
                "content": "체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": ${clubMember.id}
                  }
                ]
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/checklists")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.message").value("이미 체크리스트가 존재합니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 조회")
    @WithUserDetails(value = "hgd222@test.com")
    fun t9() {
        val jsonNode = checkListCreate(schedule.id!!)
        val checkListId = jsonNode.get("data").get("id").asLong()

        mockMvc.perform(get("/api/v1/checklists/$checkListId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("체크리스트 조회 성공"))
            .andExpect(jsonPath("$.data.id").value(checkListId))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 조회 실패 - 체크리스트가 존재하지 않는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t10() {
        val nonExistentCheckListId = 9999L

        mockMvc.perform(get("/api/v1/checklists/$nonExistentCheckListId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("체크리스트를 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 조회 실패 - 클럽 멤버가 아닌 경우")
    @WithUserDetails(value = "lyh3@test.com")
    fun t14() {
        mockMvc.perform(get("/api/v1/checklists/${schedule2.checkList!!.id}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 수정")
    @WithUserDetails(value = "hgd222@test.com")
    fun t15() {
        val jsonNode = checkListCreate(schedule.id!!)
        val checkListId = jsonNode.get("data").get("id").asLong()
        val firstItemId = jsonNode.get("data").get("checkListItems").get(0).get("id").asLong()
        val secondItemId = jsonNode.get("data").get("checkListItems").get(1).get("id").asLong()

        val anotherMember = memberRepository.findById(4L).orElseThrow()
        val anotherClubMember = ClubMember(
            member = anotherMember,
            role = ClubMemberRole.PARTICIPANT,
            state = ClubMemberState.JOINING
        )
        club.addClubMember(anotherClubMember)
        clubMemberRepository.save(anotherClubMember)

        val requestBody = """
          {
            "checkListItems": [
              {
                "id": $firstItemId,
                "content": "수정된 체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "isChecked": true,
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": ${anotherClubMember.id},
                    "isChecked": true
                  }
                ]
              },
              {
                "id": $secondItemId,
                "content": "수정된 체크리스트 아이템 2",
                "category": "${CheckListItemCategory.ETC.name}",
                "sequence": 2,
                "itemAssigns": []
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/checklists/$checkListId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("체크리스트 수정 성공"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 수정 실패 - 체크리스트가 존재하지 않는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t16() {
        val nonExistentCheckListId = 9999L

        val requestBody = """
          {
            "checkListItems": [
              {
                "id": 1,
                "content": "수정된 체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "isChecked": true,
                "sequence": 1,
                "itemAssigns": []
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/checklists/$nonExistentCheckListId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("체크리스트를 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 수정 실패 - 클럽 멤버가 아닌 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t17() {
        val jsonNode = checkListCreate(schedule.id!!)
        val checkListId = jsonNode.get("data").get("id").asLong()

        val requestBody = """
          {
            "checkListItems": [
              {
                "id": 1,
                "content": "수정된 체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "isChecked": true,
                "sequence": 1,
                "itemAssigns": [
                  {
                    "clubMemberId": 9999
                  }
                ]
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/checklists/$checkListId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("클럽 멤버를 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 수정 실패 - 호스트 또는 관리자만 체크리스트를 수정할 수 있는 경우")
    @WithUserDetails(value = "chs4s@test.com")
    fun t18() {
        val anotherMember = memberRepository.findById(2L).orElseThrow()
        val anotherClubMember = ClubMember(anotherMember, ClubMemberRole.PARTICIPANT, ClubMemberState.JOINING)

        club.addClubMember(anotherClubMember)
        clubRepository.save(club)

        val requestBody = """
          {
            "checkListItems": [
              {
                "id": 1,
                "content": "수정된 체크리스트 아이템 1",
                "category": "${CheckListItemCategory.PREPARATION.name}",
                "isChecked": true,
                "sequence": 1,
                "itemAssigns": []
              }
            ]
          }
        """.trimIndent()

        mockMvc.perform(
            put("/api/v1/checklists/${schedule2.checkList!!.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 삭제")
    @WithUserDetails(value = "hgd222@test.com")
    fun t22() {
        val jsonNode = checkListCreate(schedule.id!!)
        val checkListId = jsonNode.get("data").get("id").asLong()

        mockMvc.perform(delete("/api/v1/checklists/$checkListId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("체크리스트 삭제 성공"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 삭제 실패 - 체크리스트가 존재하지 않는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t23() {
        val nonExistentCheckListId = 9999L

        mockMvc.perform(delete("/api/v1/checklists/$nonExistentCheckListId"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("체크리스트를 찾을 수 없습니다"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 삭제 실패 - 클럽 멤버가 아닌 경우")
    @WithUserDetails(value = "chs4s@test.com")
    fun t24() {
        mockMvc.perform(delete("/api/v1/checklists/${schedule2.checkList!!.id}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 삭제 실패 - 호스트 또는 관리자만 체크리스트를 삭제할 수 있는 경우")
    @WithUserDetails(value = "chs4s@test.com")
    fun t25() {
        val anotherMember = memberRepository.findById(2L).orElseThrow()
        val anotherClubMember = ClubMember(anotherMember, ClubMemberRole.PARTICIPANT, ClubMemberState.JOINING)

        club.addClubMember(anotherClubMember)
        clubRepository.save(club)

        mockMvc.perform(delete("/api/v1/checklists/${schedule2.checkList!!.id}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 목록 조회")
    @WithUserDetails(value = "hgd222@test.com")
    fun t29() {
        mockMvc.perform(get("/api/v1/checklists/group/${club.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("체크리스트 목록 조회 성공"))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 목록 조회 실패 - 클럽이 존재하지 않는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t30() {
        mockMvc.perform(get("/api/v1/checklists/group/9999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("모임을 찾을 수 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 목록 조회 실패 - 클럽 멤버가 아닌 경우")
    @WithUserDetails(value = "chs4s@test.com")
    fun t31() {
        mockMvc.perform(get("/api/v1/checklists/group/${club.id}"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
            .andDo(print())
    }

    @Test
    @DisplayName("체크리스트 목록 조회 성공 - 클럽에 체크리스트가 없는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    fun t35() {
        mockMvc.perform(get("/api/v1/checklists/group/${club2.id}"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("체크리스트 목록 조회 성공"))
            .andExpect(jsonPath("$.data").isEmpty)
            .andDo(print())
    }
}