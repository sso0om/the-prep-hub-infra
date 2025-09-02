package com.back.domain.club.clubMember.controller

import com.back.domain.club.club.service.ClubService
import com.back.domain.club.clubMember.controller.ApiV1ClubMemberController
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.club.clubMember.service.ClubMemberService
import com.back.domain.member.member.service.MemberService
import com.back.global.aws.S3Service
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.util.function.Supplier

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
internal class ApiV1ClubMemberControllerTest{
    @Autowired
    private val mvc: MockMvc? = null

    @Autowired
    private val clubService: ClubService? = null

    @Autowired
    private val clubMemberService: ClubMemberService? = null

    @Autowired
    private val memberService: MemberService? = null

    @Autowired
    private val clubMemberRepository: ClubMemberRepository? = null

    @MockitoBean
    private val s3Service: S3Service? = null // S3Service는 MockBean으로 주입하여 실제 S3와의 통신을 피합니다.


    @Test
    @DisplayName("클럽에 멤버 추가")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun addMemberToClub() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        var club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val hostMember = memberService!!.findMemberById(1L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }


        val member2 = memberService.findMemberById(5L)
        checkNotNull(member2) { "멤버가 존재하지 않습니다." }

        // JSON 데이터 파트 생성
        val jsonData: String = """
                        {
                            "members": [
                                {
                                    "email": "${member1.getEmail()}",
                                    "role": "PARTICIPANT"
                                },
                                {
                                    "email": "${member2.getEmail()}",
                                    "role": "PARTICIPANT"
                                }
                            ]
                        }
                        
                        """.trimIndent()

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/clubs/" + club!!.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("addMembersToClub"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(201))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽에 멤버가 추가됐습니다."))

        // 추가 검증: 클럽에 멤버가 실제로 추가되었는지 확인
        club = clubService.getClubById(club.id!!)

        // 1. 전체 멤버 수 확인
        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(5) // 기존 3명 + 신규 2명

        // 2. 호스트 멤버 확인 (이건 순서가 보장된다면 그대로 둬도 무방)
        AssertionsForClassTypes.assertThat(club.clubMembers.get(0).member.getEmail()).isEqualTo(hostMember.getEmail())
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(0).role).isEqualTo(ClubMemberRole.HOST)

        // 3. 새로 추가된 멤버들이 '포함'되어 있는지, 그리고 그들의 '역할'이 올바른지 확인 (순서 무관)
        // member1 (cjw5@test.com) 검증
        AssertionsForClassTypes.assertThat(
            club.clubMembers.stream()
                .anyMatch { cm: ClubMember? ->
                    cm!!.member.getEmail() == member1.getEmail() &&
                            cm.role == ClubMemberRole.PARTICIPANT
                })
            .isTrue()

        // member2 (pms4@test.com) 검증
        AssertionsForClassTypes.assertThat(
            club.clubMembers.stream()
                .anyMatch { cm: ClubMember? ->
                    cm!!.member.getEmail() == member2.getEmail() &&
                            cm.role == ClubMemberRole.PARTICIPANT
                })
            .isTrue()
    }

    @Test
    @DisplayName("클럽에 멤버 추가 - 중복되는 멤버")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun addMemberToClub_DuplicateMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        var club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // JSON 데이터 파트 생성
        val jsonData: String = """
                {
                    "members": [
                        {
                            "email": "${member1.getEmail()}",
                            "role": "PARTICIPANT"
                        },
                        {
                            "email": "${member1.getEmail()}",
                            "role": "MANAGER"
                        }
                    ]
                }
                
                """.trimIndent()

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/clubs/" + club!!.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("addMembersToClub"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(201))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽에 멤버가 추가됐습니다."))

        // 추가 검증: 클럽에 멤버가 실제로 추가되었는지 확인
        club = clubService.getClubById(club.id!!)

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(4) // 중복된 멤버는 하나만 추가
        AssertionsForClassTypes.assertThat(club.clubMembers.get(3).member.getEmail()).isEqualTo(member1.getEmail())
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(3).role)
            .isEqualTo(ClubMemberRole.MANAGER) // 나중에 추가한 역할이 유지됨
    }

    @Test
    @DisplayName("클럽에 멤버 추가 - 이미 추가된 멤버")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun addMemberToClub_AlreadyAddedMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        var club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val hostMember = memberService!!.findMemberById(1L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }


        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService.findMemberById(3L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        AssertionsForClassTypes.assertThat(club!!.clubMembers.size).isEqualTo(3)

        // JSON 데이터 파트 생성
        val jsonData: String = """
                {
                    "members": [
                        {
                            "email": "${member1.getEmail()}",
                            "role": "PARTICIPANT"
                        }
                    ]
                }
                
                """.trimIndent()

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/clubs/" + club.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("addMembersToClub"))
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(201))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽에 멤버가 추가됐습니다."))

        // 추가 검증: 클럽에 멤버가 실제로 추가되었는지 확인
        club = clubService.getClubById(club.id!!)

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(3)
        AssertionsForClassTypes.assertThat(club.clubMembers.get(0).member.getEmail()).isEqualTo(hostMember.getEmail())
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(0).role).isEqualTo(ClubMemberRole.HOST)
        AssertionsForClassTypes.assertThat(club.clubMembers.get(2).member.getEmail()).isEqualTo(member1.getEmail())
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(2).role)
            .isEqualTo(ClubMemberRole.PARTICIPANT)
    }

    @Test
    @DisplayName("클럽에 멤버 추가 - 존재하지 않는 클럽")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun addMemberToClub_ClubNotFound() {
        // given
        val nonExistentClubId = "9999" // 존재하지 않는 클럽 ID
        val jsonData: String = """
                {
                    "members": [
                        {
                            "email": "test1@gmail.com",
                            "role": "PARTICIPANT"
                        }
                    ]
                }
                
                """.trimIndent()
        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/clubs/" + nonExistentClubId + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("addMembersToClub"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("클럽에 멤버 추가 - 존재하지 않는 멤버 이메일")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun addMemberToClub_MemberNotFound() {
        // given
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val jsonData: String = """
                {
                    "members": [
                        {
                            "email": "unknownMember@gmail.com",
                            "role": "PARTICIPANT"
                        }
                    ]
                }
                
                """.trimIndent()

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/clubs/" + club!!.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("addMembersToClub"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("클럽에 멤버 추가 - 권한 없는 멤버")
    @WithUserDetails(value = "chs4s@test.com")
    @Throws(Exception::class)
    fun addMemberToClub_UnauthorizedMember() {
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(3L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        val member2 = memberService.findMemberById(4L)
        checkNotNull(member2) { "멤버가 존재하지 않습니다." }

        // JSON 데이터 파트 생성
        val jsonData: String = """
                        {
                            "members": [
                                {
                                    "email": "${member1.getEmail()}",
                                    "role": "PARTICIPANT"
                                },
                                {
                                    "email": "${member2.getEmail()}",
                                    "role": "PARTICIPANT"
                                }
                            ]
                        }
                        
                        """.trimIndent()

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/clubs/" + club!!.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("addMembersToClub"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."))
    }

    @Test
    @DisplayName("클럽에 멤버 추가 - 클럽 정원 초과")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun addMemberToClub_ExceedMaximumCapacity() {
        // given
        // 테스트 클럽 생성
        val clubId = 2L // 테스트를 위해 클럽 ID를 2로 고정
        var club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        val member2 = memberService.findMemberById(4L)
        checkNotNull(member2) { "멤버가 존재하지 않습니다." }

        // JSON 데이터 파트 생성
        val jsonData: String = """
                {
                    "members": [
                        {
                            "email": "${member1.getEmail()}",
                            "role": "PARTICIPANT"
                        },
                        {
                            "email": "${member2.getEmail()}",
                            "role": "PARTICIPANT"
                        }
                    ]
                }
                
                """.trimIndent()

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/clubs/" + club!!.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonData)
        )
            .andDo(MockMvcResultHandlers.print())
        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("addMembersToClub"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽의 최대 멤버 수를 초과했습니다."))

        // 추가 검증: 클럽에 멤버가 실제로 추가되지 않았는지 확인
        club = clubService.getClubById(club.id!!)
        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 2명만 있어야 함 (호스트 + 참여자)
    }

    @Test
    @DisplayName("클럽 멤버 탈퇴")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun withdrawMemberFromClub() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        var club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 탈퇴할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        AssertionsForClassTypes.assertThat(club!!.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 1명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/clubs/" + club.id + "/members/" + member1.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("withdrawMemberFromClub"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽에서 멤버가 탈퇴됐습니다."))

        // 추가 검증: 클럽에서 멤버가 실제로 삭제되지 않고 state가 withdrawn로 변경되었는지 확인
        club = clubService.getClubById(club.id!!)

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 여전히 존재해야 함
        AssertionsForClassTypes.assertThat(club.clubMembers.get(1).member.getEmail()).isEqualTo(member1.getEmail())
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(1).role)
            .isEqualTo(ClubMemberRole.MANAGER)
        AssertionsForClassTypes.assertThat<ClubMemberState?>(club.clubMembers.get(1).state)
            .isEqualTo(ClubMemberState.WITHDRAWN) // 상태가 WITHDRAWN으로 변경되었는지 확인
    }

    @Test
    @DisplayName("클럽 멤버 탈퇴 - 존재하지 않는 클럽")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun withdrawMemberFromClub_ClubNotFound() {
        // given
        val nonExistentClubId = "9999" // 존재하지 않는 클럽 ID
        val memberId = 2L // 임의의 멤버 ID

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/clubs/" + nonExistentClubId + "/members/" + memberId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("withdrawMemberFromClub"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("클럽 멤버 탈퇴 - 멤버가 클럽에 존재하지 않을 때")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun withdrawMemberFromClub_MemberNotFound() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val nonExistentMemberId = 5L // 존재하지 않는 멤버 ID

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/clubs/" + club!!.id + "/members/" + nonExistentMemberId)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("withdrawMemberFromClub"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 멤버가 존재하지 않습니다."))
    }

    @Test
    @DisplayName("클럽 멤버 탈퇴 - 권한 없는 멤버")
    @WithUserDetails(value = "chs4s@test.com")
    @Throws(Exception::class)
    fun withdrawMemberFromClub_UnauthorizedMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 탈퇴할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(3L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        AssertionsForClassTypes.assertThat(club!!.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 3명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/clubs/" + club.id + "/members/" + member1.id)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("withdrawMemberFromClub"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."))
    }

    @Test
    @DisplayName("클럽 멤버 탈퇴 - 호스트가 클럽에서 탈퇴")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun withdrawHostFromClub() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/clubs/" + club!!.id + "/members/" + 1L)
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("withdrawMemberFromClub"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("호스트는 탈퇴할 수 없습니다."))
    }

    @Test
    @DisplayName("참여자 권한 변경")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun changeMemberRole() {
        // given
        // 테스트 클럽 생성

        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        var club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val hostMember = memberService!!.findMemberById(1L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }

        // 권한 변경할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService.findMemberById(3L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        AssertionsForClassTypes.assertThat(club!!.clubMembers.size).isEqualTo(3)
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(2).role)
            .isEqualTo(ClubMemberRole.PARTICIPANT) // 참여자 역할 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"MANAGER\"}")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changeMemberRole"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("멤버의 권한이 변경됐습니다."))

        // 추가 검증: 클럽에서 멤버의 역할이 실제로 변경되었는지 확인
        club = clubService.getClubById(club.id!!)

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(3)
        AssertionsForClassTypes.assertThat(club.clubMembers.get(0).member.getEmail()).isEqualTo(hostMember.getEmail())
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(0).role).isEqualTo(ClubMemberRole.HOST)
        AssertionsForClassTypes.assertThat(club.clubMembers.get(2).member.getEmail()).isEqualTo(member1.getEmail())
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(2).role)
            .isEqualTo(ClubMemberRole.MANAGER) // 역할이 MANAGER로 변경되었는지 확인
    }

    @Test
    @DisplayName("참여자 권한 변경 - 존재하지 않는 클럽")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun changeMemberRole_ClubNotFound() {
        // given
        val nonExistentClubId = "9999" // 존재하지 않는 클럽 ID
        val memberId = 2L // 임의의 멤버 ID

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/v1/clubs/" + nonExistentClubId + "/members/" + memberId + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"MANAGER\"}")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changeMemberRole"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("참여자 권한 변경 - 멤버가 클럽에 존재하지 않을 때")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun changeMemberRole_MemberNotFound() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val nonExistentMemberId = 9999L // 존재하지 않는 멤버 ID

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/v1/clubs/" + club!!.id + "/members/" + nonExistentMemberId + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"MANAGER\"}")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changeMemberRole"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("멤버가 존재하지 않습니다."))
    }

    @Test
    @DisplayName("참여자 권한 변경 - 잘못된 역할 요청")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun changeMemberRole_InvalidRole() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        AssertionsForClassTypes.assertThat(club!!.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 1명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"INVALID_ROLE\"}")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changeMemberRole"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Unknown Member role: INVALID_ROLE"))
    }

    @Test
    @DisplayName("참여자 권한 변경 - 권한 없는 멤버")
    @WithUserDetails(value = "chs4s@test.com")
    @Throws(Exception::class)
    fun changeMemberRole_UnauthorizedMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // 권한 변경할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(3L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        AssertionsForClassTypes.assertThat(club!!.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 3명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"MANAGER\"}")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changeMemberRole"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."))
    }

    @Test
    @DisplayName("참여자 권한 변경 - 호스트 본인의 권한 변경 시도")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun changeHostRole() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/v1/clubs/" + club!!.id + "/members/" + 1L + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"MANAGER\"}")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changeMemberRole"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("호스트는 본인의 역할을 변경할 수 없습니다."))
    }

    @Test
    @DisplayName("참여자 권한 변경 - 호스트 권한을 주려고 시도")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun changeMemberRole_ToHost() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 권한 변경할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.put("/api/v1/clubs/" + club!!.id + "/members/" + member1.id + "/role")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"HOST\"}")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("changeMemberRole"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("호스트 권한은 직접 부여할 수 없습니다."))
    }

    @Test
    @DisplayName("참여자 목록 반환 - state 필터 없음")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun getClubMembers() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val hostMember = memberService!!.findMemberById(1L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        // 클럽 멤버들 (testInitData의 멤버 사용)
        val member1 = memberService.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }
        val member2 = memberService.findMemberById(3L)
        checkNotNull(member2) { "멤버가 존재하지 않습니다." }

        val clubMember1 = club!!.clubMembers.get(1)
        val clubMember2 = club.clubMembers.get(2)

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/clubs/" + club.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getClubMembers"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 멤버 목록이 조회됐습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members.length()").value(3)) // 멤버가 4명인지 확인

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].memberId").value(hostMember.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].nickname").value(hostMember.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].tag").value(hostMember.tag))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].role").value(ClubMemberRole.HOST.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].email").value(hostMember.getEmail()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].memberType").value(hostMember.memberType.name))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.members[0].profileImageUrl").value("")
            ) // 호스트는 이미지 URL이 없으므로 빈 문자열
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.members[0].state").value(club.clubMembers.get(0).state.name)
            ) // 호스트의 상태 확인

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].clubMemberId").value(clubMember1.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].memberId").value(member1.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].nickname").value(member1.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].tag").value(member1.tag))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].role").value(clubMember1.role.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].email").value(member1.getEmail()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].memberType").value(member1.memberType.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].profileImageUrl").value(""))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].state").value(clubMember1.state.name))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].clubMemberId").value(clubMember2.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].memberId").value(member2.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].nickname").value(member2.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].tag").value(member2.tag))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].role").value(clubMember2.role.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].email").value(member2.getEmail()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].memberType").value(member2.memberType.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].profileImageUrl").value(""))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[2].state").value(clubMember2.state.name))
    }

    @Test
    @DisplayName("참여자 목록 반환 - state 필터 (INVITED)")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun getClubMembers_stateFiltered() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        val member1 = memberService!!.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }
        val member2 = memberService.findMemberById(3L)
        checkNotNull(member2) { "멤버가 존재하지 않습니다." }

        val clubMember1 = club!!.clubMembers.get(1) // member1의 클럽 멤버
        val clubMember2 = club.clubMembers.get(2) // member2의 클럽 멤버

        // 클럽 멤버의 상태 변경
        clubMember1.updateState(ClubMemberState.INVITED) // member2를 JOINING 상태로 변경
        clubMemberRepository!!.saveAndFlush<ClubMember?>(clubMember1) // 상태 변경된 클럽 멤버 저장

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/clubs/" + club.id + "/members" + "?state=INVITED")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getClubMembers"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 멤버 목록이 조회됐습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members.length()").value(1)) // 멤버가 3명인지 확인

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].memberId").value(member1.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].nickname").value(member1.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].tag").value(member1.tag))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].role").value(clubMember1.role.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].email").value(member1.getEmail()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].memberType").value(member1.memberType.name))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.members[0].profileImageUrl").value("")
            ) // 이미지 URL이 없으므로 빈 문자열
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.members[0].state").value(clubMember1.state.name)
            ) // 호스트의 상태 확인
    }

    @Test
    @DisplayName("참여자 목록 반환 - 존재하지 않는 클럽")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun getClubMembers_InvalidClubId() {
        // given
        val invalidClubId = 9999 // 잘못된 클럽 ID

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/clubs/" + invalidClubId + "/members")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getClubMembers"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("참여자 목록 반환 - 잘못된 state 필터")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun getClubMembers_InvalidStateFilter() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/clubs/" + club!!.id + "/members?state=INVALID_STATE")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getClubMembers"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Unknown Member state: INVALID_STATE"))
    }

    @Test
    @DisplayName("참여자 목록 반환 - WITHDRAW 상태 필터 확인")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun getClubMembers_WithdrawStateFilter() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        val hostMember = memberService!!.findMemberById(1L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }

        val member1 = memberService.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        val member2 = memberService.findMemberById(3L)
        checkNotNull(member2) { "멤버가 존재하지 않습니다." }

        val clubMember1 = club!!.clubMembers.get(1) // member1의 클럽 멤버
        val clubMember2 = club.clubMembers.get(2) // member2의 클럽 멤버


        // 클럽 멤버 상태를 WITHDRAWN으로 변경
        clubMember1.updateState(ClubMemberState.WITHDRAWN)
        clubMemberRepository!!.saveAndFlush<ClubMember?>(clubMember1) // 상태 변경된 클럽 멤버 저장

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 2명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/clubs/" + club.id + "/members")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getClubMembers"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 멤버 목록이 조회됐습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members.length()").value(2)) // 멤버가 1명(Host)인지 확인
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].memberId").value(hostMember.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].nickname").value(hostMember.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].tag").value(hostMember.tag))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].role").value(ClubMemberRole.HOST.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].email").value(hostMember.getEmail()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[0].memberType").value(hostMember.memberType.name))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.members[0].profileImageUrl").value("")
            ) // 호스트는 이미지 URL이 없으므로 빈 문자열
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.members[0].state").value(club.clubMembers.get(0).state.name)
            ) // 호스트의 상태 확인

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].clubMemberId").value(clubMember2.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].memberId").value(member2.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].nickname").value(member2.nickname))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].tag").value(member2.tag))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].role").value(clubMember2.role.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].email").value(member2.getEmail()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].memberType").value(member2.memberType.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.members[1].profileImageUrl").value(""))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.data.members[1].state").value(clubMember2.state.name)
            ) // 참여자의 상태 확인
    }

    @Test
    @DisplayName("가입 신청 수락")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun approveMemberApplication() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // 클럽에 멤버 추가 (가입 신청 상태로)
        val clubMember1 = clubMemberService!!.addMemberToClub(club!!.id!!, member1, ClubMemberRole.PARTICIPANT)
        clubMember1.updateState(ClubMemberState.APPLYING) // 가입 신청 상태로 변경
        clubMemberRepository!!.saveAndFlush<ClubMember?>(clubMember1) // 상태 변경된 클럽 멤버 저장

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(4) // 클럽에 멤버가 2명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("approveMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("가입 신청이 승인됐습니다."))

        // 클럽 멤버의 상태가 JOINING으로 변경되었는지 확인
        val updatedClubMember = clubMemberRepository.findById(clubMember1.id!!)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽 멤버가 존재하지 않습니다.") })
        AssertionsForClassTypes.assertThat<ClubMemberState?>(updatedClubMember.state).isEqualTo(ClubMemberState.JOINING)
    }

    @Test
    @DisplayName("가입 신청 수락 - 신청하지 않은 멤버")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun approveMemberApplication_NotAppliedMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/clubs/" + club!!.id + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("approveMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("가입 신청 상태가 아닙니다."))
    }

    @Test
    @DisplayName("가입 신청 수락 - 이미 가입 상태인 멤버")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun approveMemberApplication_AlreadyJoinedMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/clubs/" + club!!.id + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("approveMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 가입 상태입니다."))
    }

    @Test
    @DisplayName("가입 신청 수락 - 탈퇴 상태인 멤버")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun approveMemberApplication_WithdrawnMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // 클럽에 멤버 추가 (탈퇴 상태로)
        val clubMember1 = clubMemberService!!.addMemberToClub(club!!.id!!, member1, ClubMemberRole.PARTICIPANT)
        clubMember1.updateState(ClubMemberState.WITHDRAWN) // WITHDRAWN 상태로 변경
        clubMemberRepository!!.saveAndFlush<ClubMember?>(clubMember1) // 상태 변경된 클럽 멤버 저장

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(4) // 클럽에 멤버가 2명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("approveMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("가입 신청 상태가 아닙니다."))
    }

    @Test
    @DisplayName("가입 신청 수락 - 초대됨 상태인 멤버")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun approveMemberApplication_InvitedMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // 클럽에 멤버 추가 (초대됨 상태로)
        val clubMember1 = clubMemberService!!.addMemberToClub(club!!.id!!, member1, ClubMemberRole.PARTICIPANT)
        clubMember1.updateState(ClubMemberState.INVITED) // INVITED 상태로 변경
        clubMemberRepository!!.saveAndFlush<ClubMember?>(clubMember1) // 상태 변경된 클럽 멤버 저장

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(4) // 클럽에 멤버가 2명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("approveMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("가입 신청 상태가 아닙니다."))
    }

    @Test
    @DisplayName("가입 신청 수락 - 권한 없는 멤버")
    @WithUserDetails(value = "chs4s@test.com")
    @Throws(Exception::class)
    fun approveMemberApplication_UnauthorizedMember() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // 클럽에 멤버 추가 (가입 신청 상태로)
        val clubMember1 = clubMemberService!!.addMemberToClub(club!!.id!!, member1, ClubMemberRole.PARTICIPANT)
        clubMember1.updateState(ClubMemberState.APPLYING) // 가입 신청 상태로 변경
        clubMemberRepository!!.saveAndFlush<ClubMember?>(clubMember1) // 상태 변경된 클럽 멤버 저장

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(4)

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("approveMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("권한이 없습니다."))
    }

    @Test
    @DisplayName("가입 신청 수락 - 클럽이 존재하지 않는 경우")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun approveMemberApplication_ClubNotFound() {
        // given
        val invalidClubId = 9999L // 존재하지 않는 클럽 ID

        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(2L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/clubs/" + invalidClubId + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("approveMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("가입 신청 거절")
    @WithUserDetails(value = "hgd222@test.com")
    @Throws(Exception::class)
    fun rejectJoinRequest() {
        // given
        // 테스트 클럽 생성
        val clubId = 1L // 테스트를 위해 클럽 ID를 1로 고정
        val club = clubService!!.findClubById(clubId)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("클럽이 존재하지 않습니다.") })


        // 추가할 멤버 (testInitData의 멤버 사용)
        val member1 = memberService!!.findMemberById(4L)
        checkNotNull(member1) { "멤버가 존재하지 않습니다." }

        // 클럽에 멤버 추가 (가입 신청 상태로)
        val clubMember1 = clubMemberService!!.addMemberToClub(club!!.id!!, member1, ClubMemberRole.PARTICIPANT)
        clubMember1.updateState(ClubMemberState.APPLYING) // 가입 신청 상태로 변경
        clubMemberRepository!!.save<ClubMember?>(clubMember1) // 상태 변경된 클럽 멤버 저장

        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(4) // 클럽에 멤버가 2명 추가되었는지 확인

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/clubs/" + club.id + "/members/" + member1.id + "/approval")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1ClubMemberController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("rejectMemberApplication"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("가입 신청이 거절됐습니다."))

        // 클럽 멤버가 삭제됐는지 확인
        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(3) // 클럽에 멤버가 1명(호스트) 남아있는지 확인
        AssertionsForClassTypes.assertThat(clubMemberRepository.existsById(clubMember1.id!!))
            .isFalse() // 클럽 멤버가 삭제되었는지 확인
    }
}

