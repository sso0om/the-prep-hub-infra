package com.back.domain.member.friend.controller

import com.back.domain.member.friend.dto.FriendAddReqBody
import com.back.domain.member.friend.dto.FriendDto
import com.back.domain.member.friend.dto.FriendMemberDto
import com.back.domain.member.friend.dto.FriendStatusDto
import com.back.domain.member.friend.service.FriendService
import com.back.global.rsData.RsData
import com.back.global.rsData.RsData.Companion.of
import com.back.global.security.SecurityUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/members/me/friends")
@Tag(name = "ApiV1FriendController", description = "친구 컨트롤러")
class ApiV1FriendController(
    private val friendService: FriendService
) {
    @GetMapping
    @Operation(summary = "내 친구 목록 조회")
    fun getFriends(
        @AuthenticationPrincipal user: SecurityUser,
        @RequestParam(required = false) status: FriendStatusDto?
    ): RsData<List<FriendDto>> {
        val friendDtoList: List<FriendDto> = friendService.getFriends(user.id, status)

        return of(
            200,
            "친구 목록을 성공적으로 조회하였습니다.",
            friendDtoList
        )
    }

    @PostMapping
    @Operation(summary = "친구 추가")
    fun addFriend(
        @AuthenticationPrincipal user: SecurityUser,
        @RequestBody reqBody: @Valid FriendAddReqBody
    ): RsData<FriendDto> {
        val friendDto = friendService.addFriend(user.id, reqBody.friend_email)

        return of(
            201,
            "${reqBody.friend_email} 에게 친구 추가 요청이 성공적으로 처리되었습니다.",
            friendDto
        )
    }

    @PatchMapping("/{friendId}/accept")
    @Operation(summary = "친구 요청 수락")
    fun acceptFriend(
        @AuthenticationPrincipal user: SecurityUser,
        @PathVariable friendId: Long
    ): RsData<FriendDto> {
        val friendDto = friendService.acceptFriend(user.id, friendId)

        return of(
            200,
            "${friendDto.friendNickname}님과 친구가 되었습니다.",
            friendDto
        )
    }

    @PatchMapping("/{friendId}/reject")
    @Operation(summary = "친구 요청 거절")
    fun rejectFriend(
        @AuthenticationPrincipal user: SecurityUser,
        @PathVariable friendId: Long
    ): RsData<FriendDto> {
        val friendDto = friendService.rejectFriend(user.id, friendId)

        return of(
            200,
            "${friendDto.friendNickname}님의 친구 요청을 거절하였습니다.",
            friendDto
        )
    }

    @DeleteMapping("/{friendId}")
    @Operation(summary = "친구 삭제")
    fun deleteFriend(
        @AuthenticationPrincipal user: SecurityUser,
        @PathVariable friendId: Long
    ): RsData<FriendMemberDto> {
        val friendMemberDto = friendService.deleteFriend(user.id, friendId)

        return of(
            200,
            "${friendMemberDto.friendNickname}님이 친구 목록에서 삭제되었습니다.",
            friendMemberDto
        )
    }
}
