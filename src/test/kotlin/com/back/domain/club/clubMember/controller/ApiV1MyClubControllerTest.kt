package com.back.domain.club.clubMember.controller

import com.back.domain.club.club.entity.Club.Companion.builder
import com.back.domain.club.club.service.ClubService
import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.club.clubMember.repository.ClubMemberRepository
import com.back.domain.club.clubMember.service.ClubMemberService
import com.back.domain.member.member.service.MemberService
import com.back.global.aws.S3Service
import com.back.global.enums.ClubCategory
import com.back.global.enums.ClubMemberRole
import com.back.global.enums.ClubMemberState
import com.back.global.enums.EventType
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.function.Supplier

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
internal class ApiV1MyClubControllerTest {
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
    private val s3Service: S3Service? = null // S3Service는 MockBean으로 주입하여 실제 S3와의 통신을 피합니다

    @Test
    @DisplayName("모임 초대 수락")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun acceptClubInvitation() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )

        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 멤버를 초대 (1번을 초대)
        val invitedMember = memberService.findMemberById(1L)
        checkNotNull(invitedMember) { "초대된 멤버가 존재하지 않습니다." }

        clubMemberService.addMemberToClub(
            clubId,
            invitedMember,
            ClubMemberRole.PARTICIPANT
        )

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/my-clubs/" + club.id + "/join")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("acceptClubInvitation"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 초대를 수락했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubId").value(club.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubName").value(club.name))

        // 추가 검증: 클럽 멤버 목록에 초대된 멤버가 포함되어 있는지 확인
        AssertionsForClassTypes.assertThat(club.clubMembers.get(1).member.id).isEqualTo(invitedMember.id)
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(1).role)
            .isEqualTo(ClubMemberRole.PARTICIPANT)
        AssertionsForClassTypes.assertThat<ClubMemberState?>(club.clubMembers.get(1).state)
            .isEqualTo(ClubMemberState.JOINING)
    }

    @Test
    @DisplayName("모임 초대 거절")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun rejectClubInvitation() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 멤버를 초대 (1번을 초대)
        val invitedMember = memberService.findMemberById(1L)
        checkNotNull(invitedMember) { "초대된 멤버가 존재하지 않습니다." }

        clubMemberService.addMemberToClub(
            clubId,
            invitedMember,
            ClubMemberRole.PARTICIPANT
        )


        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/my-clubs/" + club.id + "/invitation")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("rejectClubInvitation"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 초대를 거절했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubId").value(club.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubName").value(club.name))

        // 추가 검증:
        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(1) // 초대된 멤버가 거절했으므로 클럽 멤버 수는 1명이어야 함
        AssertionsForClassTypes.assertThat(club.clubMembers.get(0).member.id).isEqualTo(hostMember.id)
    }

    @Test
    @DisplayName("모임 초대 수락 - 초대 상태가 아닌 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun acceptClubInvitation_NotInvited() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            club.id!!,
            hostMember,
            ClubMemberRole.HOST
        )


        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/my-clubs/" + club.id + "/join")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("acceptClubInvitation"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 초대 상태가 아닙니다."))
    }

    @Test
    @DisplayName("모임 초대 수락 - 이미 가입 중인 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun acceptClubInvitation_AlreadyJoined() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 이미 가입된 멤버 추가 (1번을 이미 가입 상태로 추가)
        val alreadyJoinedMember = memberService.findMemberById(1L)
        checkNotNull(alreadyJoinedMember) { "이미 가입된 멤버가 존재하지 않습니다." }

        val alreadyClubMember = clubMemberService.addMemberToClub(
            clubId,
            alreadyJoinedMember,
            ClubMemberRole.PARTICIPANT
        )

        alreadyClubMember.updateState(ClubMemberState.JOINING) // 이미 가입 상태로 업데이트

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/my-clubs/" + club.id + "/join")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("acceptClubInvitation"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 가입 상태입니다."))
    }

    @Test
    @DisplayName("모임 초대 수락 - 이미 가입 신청 중인 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun acceptClubInvitation_AlreadyApplying() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }

        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 가입 신청 중인 멤버 추가 (1번을 가입 신청 상태로 추가)
        val applyingMember = memberService.findMemberById(1L)
        checkNotNull(applyingMember) { "가입 신청 중인 멤버가 존재하지 않습니다." }

        val applyingClubMember = clubMemberService.addMemberToClub(
            clubId,
            applyingMember,
            ClubMemberRole.PARTICIPANT
        )

        applyingClubMember.updateState(ClubMemberState.APPLYING) // 가입 신청 상태로 업데이트
        clubMemberRepository!!.save<ClubMember?>(applyingClubMember)


        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/my-clubs/" + club.id + "/join")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("acceptClubInvitation"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 초대 상태가 아닙니다."))
    }

    // 잘못된 클럽
    @Test
    @DisplayName("잘못된 클럽 ID로 모임 초대 수락 시도")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun acceptClubInvitation_InvalidClubId() {
        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.patch("/api/v1/my-clubs/999/join") // 존재하지 않는 클럽 ID
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("acceptClubInvitation"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("공개 모임 가입 신청")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun applyForPublicClub() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            club.id!!,
            hostMember,
            ClubMemberRole.HOST
        )

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/my-clubs/" + club.id + "/apply")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("applyForPublicClub"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 가입 신청을 완료했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubId").value(club.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubName").value(club.name))

        // 추가 검증: 클럽 멤버 목록에 신청한 멤버가 포함되어 있는지 확인
        AssertionsForClassTypes.assertThat(club.clubMembers.get(1).member.id).isEqualTo(1L)
        AssertionsForClassTypes.assertThat<ClubMemberRole?>(club.clubMembers.get(1).role)
            .isEqualTo(ClubMemberRole.PARTICIPANT)
        AssertionsForClassTypes.assertThat<ClubMemberState?>(club.clubMembers.get(1).state)
            .isEqualTo(ClubMemberState.APPLYING)
    }

    @Test
    @DisplayName("공개 모임 가입 신청 - 이미 가입 중인 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun applyForPublicClub_AlreadyJoined() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 이미 가입된 멤버 추가 (1번을 이미 가입 상태로 추가)
        val alreadyJoinedMember = memberService.findMemberById(1L)
        checkNotNull(alreadyJoinedMember) { "이미 가입된 멤버가 존재하지 않습니다." }

        val alreadyClubMember = clubMemberService.addMemberToClub(
            clubId,
            alreadyJoinedMember,
            ClubMemberRole.PARTICIPANT
        )

        alreadyClubMember.updateState(ClubMemberState.JOINING) // 이미 가입 상태로 업데이트

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/my-clubs/" + club.id + "/apply")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("applyForPublicClub"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 가입 상태입니다."))
    }

    @Test
    @DisplayName("공개 모임 가입 신청 - 이미 가입 신청 중인 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun applyForPublicClub_AlreadyApplying() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 가입 신청 중인 멤버 추가 (1번을 가입 신청 상태로 추가)
        val applyingMember = memberService.findMemberById(1L)
        checkNotNull(applyingMember) { "가입 신청 중인 멤버가 존재하지 않습니다." }

        val applyingClubMember = clubMemberService.addMemberToClub(
            clubId,
            applyingMember,
            ClubMemberRole.PARTICIPANT
        )

        applyingClubMember.updateState(ClubMemberState.APPLYING) // 가입 신청 상태로 업데이트

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/my-clubs/" + club.id + "/apply")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("applyForPublicClub"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("이미 가입 신청 상태입니다."))
    }

    @Test
    @DisplayName("공개 모임 가입 신청 - 초대된 상태일때 에러")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun applyForPublicClub_InvitedState() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 초대된 멤버 추가 (1번을 초대 상태로 추가)
        val invitedMember = memberService.findMemberById(1L)
        checkNotNull(invitedMember) { "초대된 멤버가 존재하지 않습니다." }

        val invitedClubMember = clubMemberService.addMemberToClub(
            clubId,
            invitedMember,
            ClubMemberRole.PARTICIPANT
        )

        invitedClubMember.updateState(ClubMemberState.INVITED) // 초대 상태로 업데이트

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/my-clubs/" + club.id + "/apply")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("applyForPublicClub"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(400))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 초대 상태입니다. 초대를 수락해주세요."))
    }

    @Test
    @DisplayName("잘못된 클럽 ID로 공개 모임 가입 신청 시도")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun applyForPublicClub_InvalidClubId() {
        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/my-clubs/999/apply") // 존재하지 않는 클럽 ID
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("applyForPublicClub"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("공개 모임 가입 신청 - 클럽이 비공개인 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun applyForPublicClub_PrivateClub() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("비공개 그룹")
                .bio("비공개 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(false) // 비공개 클럽
                .leaderId(2L)
                .build()
        )

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            club.id!!,
            hostMember,
            ClubMemberRole.HOST
        )

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.post("/api/v1/my-clubs/" + club.id + "/apply")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("applyForPublicClub"))
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(403))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("비공개 클럽입니다. 가입 신청이 불가능합니다."))
    }

    @Test
    @DisplayName("클럽에서의 내 정보 반환")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun getMyClubInfo() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 멤버를 초대 (1번을 초대)
        val invitedMember = memberService.findMemberById(1L)
        checkNotNull(invitedMember) { "초대된 멤버가 존재하지 않습니다." }

        clubMemberService.addMemberToClub(
            clubId,
            invitedMember,
            ClubMemberRole.PARTICIPANT
        )

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/my-clubs/" + club.id)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getMyClubInfo"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 멤버 정보를 조회했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubId").value(club.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubName").value(club.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.role").value("PARTICIPANT"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.state").value("INVITED"))
    }

    @Test
    @DisplayName("클럽에서의 내 정보 반환 - 클럽이 존재하지 않는 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun getMyClubInfo_InvalidClubId() {
        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/my-clubs/999") // 존재하지 않는 클럽 ID
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getMyClubInfo"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("클럽에서의 내 정보 반환 - 클럽에 가입하지 않은 경우 예외 발생")
    @WithUserDetails(value = "hgd222@test.com") // 1번 멤버로 로그인
    @Throws(Exception::class)
    fun getMyClubInfo_NotJoined() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/my-clubs/" + club.id)
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getMyClubInfo"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 멤버 정보가 존재하지 않습니다."))
    }

    @Test
    @DisplayName("내 클럽 목록 반환")
    @WithUserDetails(value = "uny@test.com") // 6번 멤버로 로그인
    @Throws(Exception::class)
    fun getMyClubs() {
        // given
        val club1 = clubService!!.createClub(
            builder()
                .name("테스트 그룹 1")
                .bio("테스트 그룹 1 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId1 = club1.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        val club2 = clubService.createClub(
            builder()
                .name("테스트 그룹 2")
                .bio("테스트 그룹 2 설명")
                .category(ClubCategory.SPORTS)
                .mainSpot("부산")
                .maximumCapacity(15)
                .eventType(EventType.LONG_TERM)
                .startDate(LocalDate.of(2023, 11, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .isPublic(false)
                .leaderId(3L)
                .build()
        )
        val clubId2 = club2.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember1 = memberService!!.findMemberById(2L)
        checkNotNull(hostMember1) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId1,
            hostMember1,
            ClubMemberRole.HOST
        )

        val hostMember2 = memberService.findMemberById(3L)
        checkNotNull(hostMember2) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService.addMemberToClub(
            clubId2,
            hostMember2,
            ClubMemberRole.HOST
        )

        // 클럽에 멤버를 초대 (1번을 초대)
        val invitedMember = memberService.findMemberById(6L)
        checkNotNull(invitedMember) { "초대된 멤버가 존재하지 않습니다." }

        clubMemberService.addMemberToClub(
            clubId1,
            invitedMember,
            ClubMemberRole.PARTICIPANT
        )
        clubMemberService.addMemberToClub(
            clubId2,
            invitedMember,
            ClubMemberRole.MANAGER
        )

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/my-clubs")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getMyClubs"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("내 클럽 목록을 조회했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs.length()").value(2)) // 2개의 클럽이 있어야 함

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].clubId").value(club1.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].clubName").value(club1.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].bio").value(club1.bio))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].category").value(club1.category.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].imageUrl").value(club1.imageUrl))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].mainSpot").value(club1.mainSpot))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].eventType").value(club1.eventType.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].startDate").value(club1.startDate.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].endDate").value(club1.endDate.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].isPublic").value(club1.isPublic))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].myRole").value("PARTICIPANT"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[0].myState").value("INVITED"))

            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].clubId").value(club2.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].clubName").value(club2.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].bio").value(club2.bio))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].category").value(club2.category.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].imageUrl").value(club2.imageUrl))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].mainSpot").value(club2.mainSpot))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].eventType").value(club2.eventType.name))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].startDate").value(club2.startDate.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].endDate").value(club2.endDate.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].isPublic").value(club2.isPublic))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].myRole").value("MANAGER"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs[1].myState").value("INVITED"))
    }

    @Test
    @DisplayName("내 클럽 목록 반환 - 클럽이 없는 경우 빈 목록 반환")
    @WithUserDetails(value = "uny@test.com") // 6번 멤버로 로그인
    @Throws(Exception::class)
    fun getMyClubs_EmptyList() {
        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.get("/api/v1/my-clubs")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("getMyClubs"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("내 클럽 목록을 조회했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubs.length()").value(0)) // 빈 목록이어야 함
    }

    @Test
    @DisplayName("클럽 가입 신청 취소")
    @WithUserDetails(value = "uny@test.com") // 6번 멤버로 로그인
    @Throws(Exception::class)
    fun cancelClubApplication() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 가입 신청 중인 멤버 추가 (6번을 가입 신청 상태로 추가)
        val applyingMember = memberService.findMemberById(6L)
        checkNotNull(applyingMember) { "가입 신청 중인 멤버가 존재하지 않습니다." }

        val applyingClubMember = clubMemberService.addMemberToClub(
            clubId,
            applyingMember,
            ClubMemberRole.PARTICIPANT
        )

        applyingClubMember.updateState(ClubMemberState.APPLYING) // 가입 신청 상태로 업데이트

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/my-clubs/" + club.id + "/apply")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("cancelClubApplication"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽 가입 신청을 취소했습니다."))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubId").value(club.id))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.clubName").value(club.name))

        // 추가 검증: 클럽 멤버 목록에서 가입 신청 중인 멤버가 제거되었는지 확인
        AssertionsForClassTypes.assertThat(club.clubMembers.size).isEqualTo(1) // 호스트 멤버만 남아 있어야 함
        AssertionsForClassTypes.assertThat(club.clubMembers.get(0).member.id).isEqualTo(hostMember.id)
    }

    @Test
    @DisplayName("클럽 가입 신청 취소 - 존재하지 않는 클럽")
    @WithUserDetails(value = "uny@test.com") // 6번 멤버로 로그인
    @Throws(Exception::class)
    fun cancelClubApplication_InvalidClubId() {
        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/my-clubs/999/apply") // 존재하지 않는 클럽 ID
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("cancelClubApplication"))
            .andExpect(MockMvcResultMatchers.status().isNotFound())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(404))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("모임을 찾을 수 없습니다."))
    }

    @Test
    @DisplayName("클럽 탈퇴")
    @WithUserDetails(value = "uny@test.com") // 6번 멤버로 로그인
    @Throws(Exception::class)
    fun leaveClub() {
        // given
        val club = clubService!!.createClub(
            builder()
                .name("테스트 그룹")
                .bio("테스트 그룹 설명")
                .category(ClubCategory.STUDY)
                .mainSpot("서울")
                .maximumCapacity(10)
                .eventType(EventType.ONE_TIME)
                .startDate(LocalDate.of(2023, 10, 1))
                .endDate(LocalDate.of(2023, 10, 31))
                .isPublic(true)
                .leaderId(2L)
                .build()
        )
        val clubId = club.id ?: throw IllegalStateException("클럽 ID가 null입니다.")

        // 클럽에 호스트 멤버 추가 (2번을 호스트로)
        val hostMember = memberService!!.findMemberById(2L)
        checkNotNull(hostMember) { "호스트 멤버가 존재하지 않습니다." }
        clubMemberService!!.addMemberToClub(
            clubId,
            hostMember,
            ClubMemberRole.HOST
        )

        // 클럽에 멤버를 초대 (6번을 초대)
        val invitedMember = memberService.findMemberById(6L)
        checkNotNull(invitedMember) { "초대된 멤버가 존재하지 않습니다." }

        val clubMember = clubMemberService.addMemberToClub(
            clubId,
            invitedMember,
            ClubMemberRole.PARTICIPANT
        )
        // 클럽 멤버 상태를 JOINING으로 업데이트
        clubMember.updateState(ClubMemberState.JOINING)
        clubMemberRepository!!.save<ClubMember?>(clubMember)

        // when
        val resultActions = mvc!!.perform(
            MockMvcRequestBuilders.delete("/api/v1/my-clubs/" + club.id + "/withdraw")
        )
            .andDo(MockMvcResultHandlers.print())

        // then
        resultActions
            .andExpect(MockMvcResultMatchers.handler().handlerType(ApiV1MyClubController::class.java))
            .andExpect(MockMvcResultMatchers.handler().methodName("withdrawFromClub"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("클럽에서 탈퇴했습니다."))

        // 추가 검증: 멤버가 실제로 제거되지 않고 상태가 WITHDRAWN으로 변경되었는지 확인
        val leftClubMember = clubMemberRepository.findById(clubMember.id!!)
            .orElseThrow<IllegalStateException?>(Supplier { IllegalStateException("탈퇴한 멤버가 존재하지 않습니다.") })
        AssertionsForClassTypes.assertThat<ClubMemberState?>(leftClubMember.state).isEqualTo(ClubMemberState.WITHDRAWN)
    }
}