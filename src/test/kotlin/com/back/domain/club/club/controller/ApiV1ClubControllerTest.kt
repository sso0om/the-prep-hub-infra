package com.back.domain.club.club.controller

import com.back.domain.club.club.entity.Club
import com.back.domain.club.club.service.ClubService
import com.back.domain.member.member.dto.request.MemberRegisterDto
import com.back.domain.member.member.service.MemberService
import com.back.global.aws.S3Service
import com.back.global.enums.ClubCategory
import com.back.global.enums.EventType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.charset.StandardCharsets
import java.time.LocalDate

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiV1ClubControllerTest {

    @Autowired
    private lateinit var mvc: MockMvc

    @Autowired
    private lateinit var clubService: ClubService

    @Autowired
    private lateinit var memberService: MemberService

    @MockitoBean
    private lateinit var s3Service: S3Service // S3Service는 MockBean으로 주입하여 실제 S3와의 통신을 피합니다.

    @Test
    @DisplayName("빈 클럽 생성 - 이미지 없는 경우")
    @WithUserDetails(value = "hgd222@test.com") // 1번 유저로 로그인
    fun `createClub without image`() {
        // given
        val jsonData = """
            {
                "name": "테스트 그룹",
                "bio": "테스트 그룹 설명",
                "category" : "TRAVEL",
                "mainSpot" : "서울",
                "maximumCapacity" : 10,
                "eventType" : "SHORT_TERM",
                "startDate" : "2023-10-01",
                "endDate" : "2023-10-31",
                "isPublic": true,
                "clubMembers" : []
            }
        """.trimIndent()

        val dataPart = MockMultipartFile(
            "data",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            jsonData.toByteArray(StandardCharsets.UTF_8)
        )

        // when
        val resultActions = mvc.perform(multipart("/api/v1/clubs").file(dataPart))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(ApiV1ClubController::class.java))
            .andExpect(handler().methodName("createClub"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value("클럽이 생성됐습니다."))
            .andExpect(jsonPath("$.data.clubId").isNumber)
            .andExpect(jsonPath("$.data.leaderId").value(1))

        // 추가 검증: 그룹이 실제로 생성되었는지 확인
        val club = clubService.lastCreatedClub

        assertThat(club.name).isEqualTo("테스트 그룹")
        assertThat(club.bio).isEqualTo("테스트 그룹 설명")
        assertThat(club.category).isEqualTo(ClubCategory.TRAVEL)
        assertThat(club.mainSpot).isEqualTo("서울")
        assertThat(club.maximumCapacity).isEqualTo(10)
        assertThat(club.eventType).isEqualTo(EventType.SHORT_TERM)
        assertThat(club.startDate).isEqualTo(LocalDate.of(2023, 10, 1))
        assertThat(club.endDate).isEqualTo(LocalDate.of(2023, 10, 31))
        assertThat(club.isPublic).isTrue
        assertThat(club.leaderId).isEqualTo(1L)
        assertThat(club.state).isTrue // 활성화 상태가 true인지 확인
        assertThat(club.clubMembers).hasSize(1) // 구성원이 한명(호스트)인지 확인
    }

    @Test
    @DisplayName("빈 클럽 생성 - 이미지가 있는 경우")
    @WithUserDetails(value = "hgd222@test.com") // 1번 유저로 로그인
    fun `createClub with image`() {
        // given
        // ⭐️ S3 업로더의 행동 정의: 어떤 파일이든 업로드 요청이 오면, 지정된 가짜 URL을 반환한다.
        val fakeImageUrl = "https://my-s3-bucket.s3.ap-northeast-2.amazonaws.com/club/1/profile/fake-image.jpg"
        given(s3Service.upload(any<MultipartFile>(), any<String>())).willReturn(fakeImageUrl)

        // 1. 가짜 이미지 파일(MockMultipartFile) 생성
        val imagePart = MockMultipartFile(
            "image", // @RequestPart("image") 이름과 일치
            "image.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test image".toByteArray()
        )

        // 2. JSON 데이터 파트 생성
        val jsonData = """
            {
                "name": "이미지 있는 그룹",
                "bio": "테스트 그룹 설명",
                "category" : "HOBBY",
                "mainSpot" : "부산",
                "maximumCapacity" : 5,
                "eventType" : "LONG_TERM",
                "startDate" : "2025-08-01",
                "endDate" : "2026-07-31",
                "isPublic": false,
                "clubMembers" : []
            }
        """.trimIndent()
        val dataPart = MockMultipartFile("data", "", "application/json", jsonData.toByteArray(StandardCharsets.UTF_8))

        // when
        // 3. MockMvc로 multipart 요청 생성 (JSON 파트와 이미지 파트 모두 포함)
        val resultActions = mvc.perform(
            multipart("/api/v1/clubs")
                .file(dataPart)
                .file(imagePart) // 'image' 파트 추가
        )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.clubId").isNumber)

        // 추가 검증
        val club = clubService.lastCreatedClub
        assertThat(club.name).isEqualTo("이미지 있는 그룹")
        assertThat(club.imageUrl).isEqualTo(fakeImageUrl) // ⭐️ 이미지 URL이 가짜 URL과 일치하는지 확인
    }

    @Test
    @DisplayName("초기 유저 있는 클럽 생성")
    @WithUserDetails(value = "hgd222@test.com") // 1번 유저로 로그인
    fun `createClub with initial members`() {
        // given
        val jsonData = """
            {
                "name": "테스트 그룹",
                "bio": "테스트 그룹 설명",
                "category" : "TRAVEL",
                "mainSpot" : "서울",
                "maximumCapacity" : 10,
                "eventType" : "SHORT_TERM",
                "startDate" : "2023-10-01",
                "endDate" : "2023-10-31",
                "isPublic": true,
                "clubMembers" : [
                    {"id": 2, "role" : "MANAGER"},
                    {"id": 3, "role" : "PARTICIPANT"},
                    {"id": 4, "role" : "PARTICIPANT"}
                ]
            }
        """.trimIndent()

        val dataPart = MockMultipartFile(
            "data",
            "",
            MediaType.APPLICATION_JSON_VALUE,
            jsonData.toByteArray(StandardCharsets.UTF_8)
        )

        // when
        val resultActions = mvc.perform(multipart("/api/v1/clubs").file(dataPart))
            .andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(ApiV1ClubController::class.java))
            .andExpect(handler().methodName("createClub"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value("클럽이 생성됐습니다."))
            .andExpect(jsonPath("$.data.clubId").isNumber)
            .andExpect(jsonPath("$.data.leaderId").value(1))

        // 추가 검증
        val club = clubService.lastCreatedClub

        assertThat(club.name).isEqualTo("테스트 그룹")
        assertThat(club.clubMembers).hasSize(4) // 클럽 멤버가 4명인지 확인

        assertThat(club.clubMembers[0].role.name).isEqualTo("HOST")
        assertThat(club.clubMembers[0].member.id).isEqualTo(1L)
        assertThat(club.clubMembers[1].role.name).isEqualTo("MANAGER")
        assertThat(club.clubMembers[1].member.id).isEqualTo(2L)
        assertThat(club.clubMembers[2].role.name).isEqualTo("PARTICIPANT")
        assertThat(club.clubMembers[2].member.id).isEqualTo(3L)
        assertThat(club.clubMembers[3].role.name).isEqualTo("PARTICIPANT")
        assertThat(club.clubMembers[3].member.id).isEqualTo(4L)
    }

    @Test
    @DisplayName("클럽 정보 수정")
    @WithUserDetails(value = "hgd222@test.com") // 1번 유저로 로그인
    fun `updateClub info`() {
        // given
        val clubId = 1L

        // ⭐️ S3 업로더의 행동 정의
        val fakeImageUrl = "https://my-s3-bucket.s3.ap-northeast-2.amazonaws.com/club/1/profile/fake-image.jpg"
        given(s3Service.upload(any<MultipartFile>(), any<String>())).willReturn(fakeImageUrl)

        val imagePart = MockMultipartFile(
            "image", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".toByteArray()
        )

        val jsonData = """
            {
                "name": "수정된 테스트 그룹",
                "bio": "수정된 테스트 그룹 설명",
                "category" : "HOBBY",
                "mainSpot" : "수정된 서울",
                "maximumCapacity" : 11,
                "recruitingStatus": false,
                "eventType" : "LONG_TERM",
                "startDate" : "2024-10-01",
                "endDate" : "2024-10-31",
                "isPublic": true
            }
        """.trimIndent()
        val dataPart = MockMultipartFile("data", "", "application/json", jsonData.toByteArray(StandardCharsets.UTF_8))

        // when
        val resultActions = mvc.perform(
            multipart("/api/v1/clubs/$clubId")
                .file(dataPart)
                .file(imagePart)
                .with { request ->
                    request.method = "PATCH"
                    request
                }
        ).andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(ApiV1ClubController::class.java))
            .andExpect(handler().methodName("updateClubInfo"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("클럽 정보가 수정됐습니다."))
            .andExpect(jsonPath("$.data.clubId").value(clubId))

        // 추가 검증
        val updatedClub = clubService.getClubById(clubId)

        assertThat(updatedClub.name).isEqualTo("수정된 테스트 그룹")
        assertThat(updatedClub.bio).isEqualTo("수정된 테스트 그룹 설명")
        assertThat(updatedClub.category).isEqualTo(ClubCategory.HOBBY)
        assertThat(updatedClub.mainSpot).isEqualTo("수정된 서울")
        assertThat(updatedClub.maximumCapacity).isEqualTo(11)
        assertThat(updatedClub.imageUrl).isEqualTo(fakeImageUrl)
        assertThat(updatedClub.recruitingStatus).isFalse
    }

    @Test
    @DisplayName("클럽 정보 수정 - 부분 수정")
    @WithUserDetails(value = "hgd222@test.com") // 1번 유저로 로그인
    fun `updateClub info partially`() {
        // given
        val clubId = 1L
        val originalClub = clubService.findClubById(clubId)
            .orElseThrow { IllegalStateException("클럽이 존재하지 않습니다.") }

        val fakeImageUrl = "https://my-s3-bucket.s3.ap-northeast-2.amazonaws.com/club/1/profile/fake-image.jpg"
        given(s3Service.upload(any<MultipartFile>(), any<String>())).willReturn(fakeImageUrl)

        val imagePart = MockMultipartFile("image", "image.jpg", MediaType.IMAGE_JPEG_VALUE, "test image".toByteArray())

        val jsonData = """
            {
                "name": "수정된 테스트 그룹",
                "maximumCapacity" : 11,
                "recruitingStatus": false
            }
        """.trimIndent()
        val dataPart = MockMultipartFile("data", "", "application/json", jsonData.toByteArray(StandardCharsets.UTF_8))

        // when
        mvc.perform(
            multipart("/api/v1/clubs/$clubId")
                .file(dataPart)
                .file(imagePart)
                .with { it.method = "PATCH"; it }
        ).andDo(print())
            .andExpect(status().isOk)

        // then
        val updatedClub = clubService.getClubById(clubId)
        assertThat(updatedClub.name).isEqualTo("수정된 테스트 그룹")
        assertThat(updatedClub.maximumCapacity).isEqualTo(11)
        assertThat(updatedClub.recruitingStatus).isFalse
        assertThat(updatedClub.imageUrl).isEqualTo(fakeImageUrl)
        // 수정되지 않은 필드 검증
        assertThat(updatedClub.bio).isEqualTo(originalClub?.bio)
        assertThat(updatedClub.category).isEqualTo(originalClub?.category)
    }

    @Test
    @DisplayName("클럽 수정 - 존재하지 않는 클럽")
    @WithUserDetails(value = "hgd222@test.com")
    fun `update non-existent club`() {
        // given
        val nonExistentClubId = 999L
        val dataPart = MockMultipartFile("data", "", "application/json", "{}".toByteArray())

        // when & then
        mvc.perform(
            multipart("/api/v1/clubs/$nonExistentClubId")
                .file(dataPart)
                .with { it.method = "PATCH"; it }
        ).andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("클럽 수정 - 권한 없는 유저")
    @WithUserDetails(value = "lyh3@test.com") // 3번 유저로 로그인
    fun `update club without permission`() {
        // given
        val clubId = 1L
        val dataPart = MockMultipartFile("data", "", "application/json", "{}".toByteArray())

        // when & then
        mvc.perform(
            multipart("/api/v1/clubs/$clubId")
                .file(dataPart)
                .with { it.method = "PATCH"; it }
        ).andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
    }


    @Test
    @DisplayName("클럽 정보 삭제")
    @WithUserDetails(value = "hgd222@test.com") // 1번 유저로 로그인
    fun `deleteClub`() {
        // given
        val clubId = 1L

        // when
        val resultActions = mvc.perform(
            multipart("/api/v1/clubs/$clubId")
                .with { request ->
                    request.method = "DELETE"
                    request
                }
        ).andDo(print())

        // then
        resultActions
            .andExpect(handler().handlerType(ApiV1ClubController::class.java))
            .andExpect(handler().methodName("deleteClub"))
            .andExpect(status().isNoContent)
            .andExpect(jsonPath("$.code").value(204))
            .andExpect(jsonPath("$.message").value("클럽이 삭제됐습니다."))

        // 추가 검증: state가 false로 변경됐는지 확인
        val club = clubService.getClubById(clubId)
        assertThat(club.state).isFalse
    }

    @Test
    @DisplayName("클럽 정보 삭제 - 존재하지 않는 클럽")
    @WithUserDetails(value = "hgd222@test.com")
    fun `delete non-existent club`() {
        // given
        val nonExistentClubId = 999L

        // when & then
        mvc.perform(
            multipart("/api/v1/clubs/$nonExistentClubId")
                .with { it.method = "DELETE"; it }
        ).andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("클럽 정보 삭제 - 권한 없는 유저")
    @WithUserDetails(value = "lyh3@test.com") // 3번 유저로 로그인
    fun `delete club without permission`() {
        // given
        val clubId = 1L

        // when & then
        mvc.perform(
            multipart("/api/v1/clubs/$clubId")
                .with { it.method = "DELETE"; it }
        ).andDo(print())
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.message").value("권한이 없습니다."))
    }


    @Test
    @DisplayName("클럽 정보 조회")
    fun `get club info`() {
        // given
        val dto = MemberRegisterDto("testLeader@gmail.com", "12345678", "testLeader", "I'm a test leader")
        memberService.registerMember(dto)
        val member = memberService.findMemberByEmail(dto.email)

        val club = clubService.createClub(
            Club(
                name = "테스트 그룹",
                bio = "테스트 그룹 설명",
                category = ClubCategory.STUDY,
                mainSpot = "서울",
                maximumCapacity = 10,
                eventType = EventType.ONE_TIME,
                startDate = LocalDate.of(2023, 10, 1),
                endDate = LocalDate.of(2023, 10, 31),
                imageUrl = "https://example.com/image.jpg",
                isPublic = true,
                leaderId = member.id
            )
        )

        // when
        val resultActions = mvc.perform(
            multipart("/api/v1/clubs/${club.id}")
                .with { it.method = "GET"; it }
        ).andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("클럽 정보가 조회됐습니다."))
            .andExpect(jsonPath("$.data.clubId").value(club.id))
            .andExpect(jsonPath("$.data.name").value(club.name))
            .andExpect(jsonPath("$.data.leaderName").value(dto.nickname))
    }

    @Test
    @DisplayName("클럽 정보 조회 - 존재하지 않는 클럽")
    fun `get non-existent club info`() {
        // given
        val nonExistentClubId = 999L

        // when & then
        mvc.perform(
            multipart("/api/v1/clubs/$nonExistentClubId")
                .with { it.method = "GET"; it }
        ).andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("해당 ID의 클럽을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("공개 클럽 목록 조회")
    fun `get public club list`() {
        // given
        // testinitdata의 club 정보 이용
        val club1 = clubService.getClubById(1L)
        val club4 = clubService.getClubById(4L)

        // when
        val resultActions = mvc.perform(
            get("/api/v1/clubs/public")
                .param("page", "0")
                .param("size", "20")
        ).andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("공개 클럽 목록이 조회됐습니다."))
            .andExpect(jsonPath("$.data.content.length()").value(2)) // 공개 클럽은 두 개
            .andExpect(jsonPath("$.data.content[0].clubId").value(club1.id))
            .andExpect(jsonPath("$.data.content[0].name").value(club1.name))
            .andExpect(jsonPath("$.data.content[1].clubId").value(club4.id))
            .andExpect(jsonPath("$.data.content[1].name").value(club4.name))
    }
}