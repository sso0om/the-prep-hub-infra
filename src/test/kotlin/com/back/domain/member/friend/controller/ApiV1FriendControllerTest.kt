package com.back.domain.member.friend.controller

import com.back.domain.member.friend.dto.FriendDto
import com.back.domain.member.friend.dto.FriendStatusDto
import com.back.domain.member.friend.entity.Friend
import com.back.domain.member.friend.error.FriendErrorCode
import com.back.domain.member.friend.repository.FriendRepository
import com.back.domain.member.friend.service.FriendService
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.error.MemberErrorCode
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ErrorCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@ActiveProfiles("test")
@SpringBootTest
@Transactional
@AutoConfigureMockMvc
internal class ApiV1FriendControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var friendService: FriendService

    @Autowired
    private lateinit var friendRepository: FriendRepository

    @Test
    @DisplayName("친구 목록 조회")
    @WithUserDetails("hgd222@test.com")
    fun trl1() {
        // given
        val memberId = 1L
        val friends = friendService.getFriends(memberId, FriendStatusDto.ACCEPTED)

        // when
        val resultActions = mockMvc
            .perform(
            get("/api/v1/members/me/friends")
                .param("status", "ACCEPTED")
            )
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName("getFriends"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("친구 목록을 성공적으로 조회하였습니다."))
            .andExpectFriendList(friends)
    }

    @Test
    @DisplayName("친구 추가")
    @WithUserDetails("hgd222@test.com")
    fun tc1() {
        // given
        val friendMemberId = 2L
        val friendMember = memberRepository.findById(friendMemberId).orElseThrow()
        val friendEmail = friendMember.getMemberInfo()!!.email

        // when
        val resultActions = mockMvc
            .perform(
            post("/api/v1/members/me/friends")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"friend_email": "$friendEmail"}""")
            )
            .andDo(print())

        // then
        val me = memberRepository.findById(1L).orElseThrow()
        val friendEntity = friendRepository.findFirstByRequestedByOrderByIdDesc(me)!!

        resultActions
            .andExpect(status().isCreated)
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName("addFriend"))
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.message").value("$friendEmail 에게 친구 추가 요청이 성공적으로 처리되었습니다."))
            .andExpectFriendRequestDetails(friendEntity, friendMember)
    }

    @Test
    @DisplayName("친구 추가 - 친구 대상이 없는 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun tc2() {
        performErrAddFriend("a@test.com", MemberErrorCode.MEMBER_NOT_FOUND)
    }

    @Test
    @DisplayName("친구 추가 - 자기 자신을 친구로 추가하는 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun tc3() {
        performErrAddFriend("hgd222@test.com", FriendErrorCode.FRIEND_REQUEST_SELF)
    }

    @Test
    @DisplayName("친구 추가 - 이미 친구 요청을 보낸 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun tc4() {
        performErrAddFriend("lyh3@test.com", FriendErrorCode.FRIEND_ALREADY_REQUEST_PENDING)
    }

    @Test
    @DisplayName("친구 추가 - 이미 친구 요청을 받은 경우 예외 처리")
    @WithUserDetails("lyh3@test.com")
    fun tc5() {
        performErrAddFriend("hgd222@test.com", FriendErrorCode.FRIEND_ALREADY_RESPOND_PENDING)
    }

    @Test
    @DisplayName("친구 추가 - 이미 친구인 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun tc6() {
        performErrAddFriend("cjw5@test.com", FriendErrorCode.FRIEND_ALREADY_ACCEPTED)
    }

    @Test
    @DisplayName("친구 추가 - 이전에 거절 당한 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun tc7() {
        performErrAddFriend("pms4@test.com", FriendErrorCode.FRIEND_ALREADY_REJECTED)
    }

    @Test
    @DisplayName("친구 수락")
    @WithUserDetails("lyh3@test.com")
    fun ta1() {
        // given
        val friendId = 1L
        val friend = friendRepository.findById(friendId).orElseThrow()
        val friendMember = friend.requestedBy

        // when
        val resultActions = mockMvc
            .perform(patch("/api/v1/members/me/friends/$friendId/accept"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName("acceptFriend"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("${friendMember.nickname}님과 친구가 되었습니다."))
            .andExpectAcceptedOrRejectedFriendDetails(friend, friendMember, "ACCEPTED")
    }

    @Test
    @DisplayName("친구 수락 - 친구 요청이 없는 경우 예외 처리")
    @WithUserDetails("lyh3@test.com")
    fun ta2() {
        performErrPatchFriend(
            100L,
            "accept",
            "acceptFriend",
            FriendErrorCode.FRIEND_NOT_FOUND
        )
    }

    @Test
    @DisplayName("친구 수락 - 친구 엔티티는 있으나 로그인 회원과 관련 없을 경우 예외 처리")
    @WithUserDetails("chs4s@test.com")
    fun ta3() {
        performErrPatchFriend(
            1L,
            "accept",
            "acceptFriend",
            FriendErrorCode.FRIEND_ACCESS_DENIED
        )
    }

    @Test
    @DisplayName("친구 수락 - 받는이가 아닌 요청자가 친구 요청을 수락하는 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun ta4() {
        performErrPatchFriend(
            1L,
            "accept",
            "acceptFriend",
            FriendErrorCode.FRIEND_REQUEST_NOT_ALLOWED_ACCEPT
        )
    }

    @Test
    @DisplayName("친구 수락 - 이미 친구인 경우 예외 처리")
    @WithUserDetails("cjw5@test.com")
    fun ta5() {
        performErrPatchFriend(
            2L,
            "accept",
            "acceptFriend",
            FriendErrorCode.FRIEND_ALREADY_ACCEPTED
        )
    }

    @Test
    @DisplayName("친구 요청 거절")
    @WithUserDetails("lyh3@test.com")
    fun trj1() {
        // given
        val friendId = 1L
        val friend = friendRepository.findById(friendId).orElseThrow()
        val friendMember = friend.requestedBy

        // when
        val resultActions = mockMvc
            .perform(patch("/api/v1/members/me/friends/$friendId/reject"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName("rejectFriend"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("${friendMember.nickname}님의 친구 요청을 거절하였습니다."))
            .andExpectAcceptedOrRejectedFriendDetails(friend, friendMember, "REJECTED")
    }

    @Test
    @DisplayName("친구 거절 - 친구 요청이 없는 경우 예외 처리")
    @WithUserDetails("lyh3@test.com")
    fun trj2() {
        performErrPatchFriend(
            100L,
            "reject",
            "rejectFriend",
            FriendErrorCode.FRIEND_NOT_FOUND
        )
    }

    @Test
    @DisplayName("친구 거절 - 친구 엔티티는 있으나 로그인 회원과 관련 없을 경우 예외 처리")
    @WithUserDetails("chs4s@test.com")
    fun trj3() {
        performErrPatchFriend(
            1L,
            "reject",
            "rejectFriend",
            FriendErrorCode.FRIEND_ACCESS_DENIED
        )
    }

    @Test
    @DisplayName("친구 거절 - 받는이가 아닌 요청자가 친구 요청을 거절하는 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun trj4() {
        performErrPatchFriend(
            1L,
            "reject",
            "rejectFriend",
            FriendErrorCode.FRIEND_REQUEST_NOT_ALLOWED_REJECT
        )
    }

    @Test
    @DisplayName("친구 거절 - 이미 친구인 경우 예외 처리")
    @WithUserDetails("cjw5@test.com")
    fun trj5() {
        performErrPatchFriend(
            2L,
            "reject",
            "rejectFriend",
            FriendErrorCode.FRIEND_ALREADY_ACCEPTED
        )
    }

    @Test
    @DisplayName("친구 삭제")
    @WithUserDetails("hgd222@test.com")
    fun td1() {
        // given
        val friendId = 2L
        val me = memberRepository.findById(1L).orElseThrow()
        val friend = friendRepository.findById(friendId).orElseThrow()
        val friendMember = friend.getOther(me)

        // when
        val resultActions = mockMvc
            .perform(delete("/api/v1/members/me/friends/$friendId"))
            .andDo(print())

        // then
        resultActions
            .andExpect(status().isOk)
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName("deleteFriend"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.message").value("${friendMember.nickname}님이 친구 목록에서 삭제되었습니다."))
            .andExpectDeletedFriendDetails(friendMember)
    }

    @Test
    @DisplayName("친구 삭제 - 친구 요청이 없는 경우")
    @WithUserDetails("hgd222@test.com")
    fun td2() {
        performErrDelFriend(100L, FriendErrorCode.FRIEND_NOT_FOUND)
    }

    @Test
    @DisplayName("친구 삭제 - 친구 요청이 수락되지 않은 경우 예외 처리")
    @WithUserDetails("hgd222@test.com")
    fun td3() {
        performErrDelFriend(1L, FriendErrorCode.FRIEND_NOT_ACCEPTED)
    }


    // ========== 헬퍼 메서드 ==========
    
    /**
     * 친구 추가 요청 예외 처리하는 메서드
     * @param friendEmail       친구의 이메일
     * @param expectedErrorCode 예상되는 HTTP 상태 코드, 에러 메시지
     */
    private fun performErrAddFriend(friendEmail: String, expectedErrorCode: ErrorCode) {
        mockMvc
            .perform(
            post("/api/v1/members/me/friends")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"friend_email": "$friendEmail"}""")
            )
            .andDo(print())
            .andExpect(status().`is`(expectedErrorCode.status))
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName("addFriend"))
            .andExpect(jsonPath("$.code").value(expectedErrorCode.status))
            .andExpect(jsonPath("$.message").value(expectedErrorCode.message))
    }

    /**
     * 친구 요청 수락, 거절 예외 처리하는 메서드
     * @param friendId          친구 엔티티 ID
     * @param pathUrl           URL 경로 (accept, reject 등)
     * @param methodName        메서드 이름
     * @param expectedErrorCode 예상되는 HTTP 상태 코드, 에러 메시지
     */
    private fun performErrPatchFriend(friendId: Long, pathUrl: String, methodName: String, expectedErrorCode: ErrorCode) {
        mockMvc
            .perform(patch("/api/v1/members/me/friends/$friendId/$pathUrl"))
            .andDo(print())
            .andExpect(status().`is`(expectedErrorCode.status))
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName(methodName))
            .andExpect(jsonPath("$.code").value(expectedErrorCode.status))
            .andExpect(jsonPath("$.message").value(expectedErrorCode.message))
    }

    /**
     * 친구 삭제 요청 예외 처리하는 메서드
     * @param friendId          친구 엔티티 ID
     * @param expectedErrorCode 예상되는 HTTP 상태 코드, 에러 메시지
     */
    private fun performErrDelFriend(friendId: Long, expectedErrorCode: ErrorCode) {
        mockMvc
            .perform(delete("/api/v1/members/me/friends/$friendId"))
            .andDo(print())
            .andExpect(status().`is`(expectedErrorCode.status))
            .andExpect(handler().handlerType(ApiV1FriendController::class.java))
            .andExpect(handler().methodName("deleteFriend"))
            .andExpect(jsonPath("$.code").value(expectedErrorCode.status))
            .andExpect(jsonPath("$.message").value(expectedErrorCode.message))
    }

    
    // ========== 확장 함수 ==========
    
    private fun ResultActions.andExpectFriendList(friends: List<FriendDto>): ResultActions {
        friends.forEachIndexed { i, friend ->
            andExpect(jsonPath("$.data[$i].friendId").value(friend.friendId))
            andExpect(jsonPath("$.data[$i].friendMemberId").value(friend.friendMemberId))
            andExpect(jsonPath("$.data[$i].friendNickname").value(friend.friendNickname))
            andExpect(jsonPath("$.data[$i].friendProfileImageUrl").value(friend.friendProfileImageUrl))
            andExpect(jsonPath("$.data[$i].status").value(friend.status.name))
        }
        return this
    }

    private fun ResultActions.andExpectFriendRequestDetails(friend: Friend, friendMember: Member): ResultActions {
        val friendMemberInfo = friendMember.getMemberInfo()!!
        return andExpect(jsonPath("$.data.friendId").value(friend.id))
            .andExpect(jsonPath("$.data.friendMemberId").value(friendMember.id))
            .andExpect(jsonPath("$.data.friendNickname").value(friendMember.nickname))
            .andExpect(jsonPath("$.data.friendBio").value(friendMemberInfo.bio))
            .andExpect(jsonPath("$.data.friendProfileImageUrl").value(friendMemberInfo.profileImageUrl))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
    }

    private fun ResultActions.andExpectAcceptedOrRejectedFriendDetails(friend: Friend, friendMember: Member, status: String): ResultActions {
        val friendMemberInfo = friendMember.getMemberInfo()!!
        return andExpect(jsonPath("$.data.friendId").value(friend.id))
            .andExpect(jsonPath("$.data.friendNickname").value(friendMember.nickname))
            .andExpect(jsonPath("$.data.friendBio").value(friendMemberInfo.bio))
            .andExpect(jsonPath("$.data.friendProfileImageUrl").value(friendMemberInfo.profileImageUrl))
            .andExpect(jsonPath("$.data.status").value(status))
    }

    private fun ResultActions.andExpectDeletedFriendDetails(friendMember: Member): ResultActions {
        val friendMemberInfo = friendMember.getMemberInfo()!!
        return andExpect(jsonPath("$.data.friendNickname").value(friendMember.nickname))
            .andExpect(jsonPath("$.data.friendBio").value(friendMemberInfo.bio))
            .andExpect(jsonPath("$.data.friendProfileImageUrl").value(friendMemberInfo.profileImageUrl))
    }
}