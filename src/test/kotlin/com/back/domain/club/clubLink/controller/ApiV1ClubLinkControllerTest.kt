package com.back.domain.club.clubLink.controller

import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.repository.ClubRepository
import com.back.domain.club.clubLink.entity.ClubLink
import com.back.domain.club.clubLink.repository.ClubLinkRepository
import com.back.domain.club.clubLink.service.ClubLinkService
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.repository.MemberRepository
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1ClubLinkControllerTest @Autowired constructor(
    private val clubLinkService: ClubLinkService,
    private val clubRepository: ClubRepository,
    private val clubMemberRepository: ClubMemberRepository,
    private val clubLinkRepository: ClubLinkRepository,
    private val memberRepository: MemberRepository,
    private val mockMvc: MockMvc
) {

    @Value("\${app.frontend.base-url}")
    lateinit var frontendBaseUrl: String

    @BeforeEach
    fun setup() {
        // 기존 멤버가 클럽에 있으면 제거
        clubMemberRepository.findAll().forEach { cm ->
            if (cm.club!!.id == 2L && cm.member.id == 7L) {
                clubMemberRepository.delete(cm)
            }
        }
    }

    private fun findMemberByEmail(email: String): Member =
        memberRepository.findByMemberInfo_Email(email)
            ?: throw IllegalArgumentException("멤버를 찾을 수 없습니다: $email")

    @Test
    @DisplayName("초대 링크 생성 - 링크 생성 성공")
    @WithUserDetails("hgd222@test.com")
    fun createClubLink_Success() {
        val result: MvcResult = mockMvc.perform(post("/api/v1/clubs/1/members/invitation-link"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("클럽 초대 링크가 생성되었습니다."))
            .andExpect(jsonPath("$.data.link").exists())
            .andExpect(jsonPath("$.data.link").value(containsString("$frontendBaseUrl/clubs/invite?token=")))
            .andReturn()

        val root: JsonNode = ObjectMapper().readTree(result.response.contentAsString)
        val link = root.path("data").path("link").asText()
        val inviteCodeFromResponse = link.split("?")
            .getOrNull(1)
            ?.split("&")
            ?.firstOrNull { it.startsWith("token=") }
            ?.substringAfter("token=")

        assertNotNull(inviteCodeFromResponse)

        val club: Club = clubRepository.findById(1L).orElseThrow()
        val savedLink: ClubLink? = clubLinkRepository.findByClubAndExpiresAtAfter(club, LocalDateTime.now())
        assertNotNull(savedLink) {
            assertEquals(inviteCodeFromResponse, it.inviteCode)
        }
    }

    @Test
    @DisplayName("초대 링크 생성 실패 - 존재하지 않는 클럽 ID")
    @WithUserDetails("hgd222@test.com")
    fun createClubLink_Fail_ClubNotFound() {
        mockMvc.perform(post("/api/v1/clubs/9999999/members/invitation-link"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("해당 id의 클럽을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("초대 링크 생성 실패 - 권한 없는 멤버")
    @WithUserDetails("lyh3@test.com")
    fun createClubLink_Fail_NoPermission() {
        mockMvc.perform(post("/api/v1/clubs/1/members/invitation-link"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("호스트나 매니저만 초대 링크를 관리할 수 있습니다."))
    }

    @Test
    @DisplayName("초대 링크 생성 - 기존 유효한 링크가 존재할 경우 해당 링크 반환")
    @WithUserDetails("hgd222@test.com")
    fun createClubLink_ExistingLink_Returned() {
        val club: Club = clubRepository.findById(1L).orElseThrow()
        val existingCode = "existing-code-123"

        val existingLink = ClubLink(
            id = null,
            inviteCode = existingCode,
            createdAt = LocalDateTime.now().minusDays(1),
            expiresAt = LocalDateTime.now().plusDays(7),
            club = club
        )
        clubLinkRepository.save(existingLink)

        mockMvc.perform(post("/api/v1/clubs/1/members/invitation-link"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("클럽 초대 링크가 생성되었습니다."))
            .andExpect(jsonPath("$.data.link").value(containsString(existingCode)))
    }

    @Test
    @DisplayName("초대 링크 조회 성공 - 유효한 초대 링크가 존재하면 반환")
    @WithUserDetails("hgd222@test.com")
    fun getExistingClubLink_success() {
        val club: Club = clubRepository.findById(1L).orElseThrow()
        val inviteCode = "valid-code-456"

        val clubLink = ClubLink(
            id = null,
            inviteCode = inviteCode,
            createdAt = LocalDateTime.now().minusDays(1),
            expiresAt = LocalDateTime.now().plusDays(7),
            club = club
        )
        clubLinkRepository.save(clubLink)

        mockMvc.perform(get("/api/v1/clubs/1/members/invitation-link"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("클럽 초대 링크가 반환되었습니다."))
            .andExpect(jsonPath("$.data.link").value(containsString(inviteCode)))
    }

    @Test
    @DisplayName("초대 링크 조회 실패 - 유효한 링크가 없을 때 예외 발생")
    @WithUserDetails("hgd222@test.com")
    fun getExistingClubLink_fail_noLink() {
        val club: Club = clubRepository.findById(1L).orElseThrow()
        clubLinkRepository.deleteAll()

        mockMvc.perform(get("/api/v1/clubs/1/members/invitation-link"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("활성화된 초대 링크를 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("초대 링크 조회 실패 - 권한 없는 멤버일 경우 예외 발생")
    @WithUserDetails("lyh3@test.com")
    fun getExistingClubLink_fail_noPermission() {
        mockMvc.perform(get("/api/v1/clubs/1/members/invitation-link"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("호스트나 매니저만 초대 링크를 관리할 수 있습니다."))
    }

    @Test
    @DisplayName("초대 링크 조회 실패 - 존재하지 않는 클럽 ID")
    @WithUserDetails("hgd222@test.com")
    fun getExistingClubLink_fail_clubNotFound() {
        mockMvc.perform(get("/api/v1/clubs/9999999/members/invitation-link"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("해당 id의 클럽을 찾을 수 없습니다."))
    }
}
