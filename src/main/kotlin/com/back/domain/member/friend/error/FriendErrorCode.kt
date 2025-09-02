package com.back.domain.member.friend.error

import com.back.global.exception.ErrorCode

enum class FriendErrorCode(
    override val status: Int,
    override val message: String
) : ErrorCode {
    // 404
    FRIEND_NOT_FOUND(404, "친구 요청이 존재하지 않습니다."),
    FRIEND_MEMBER_NOT_FOUND(404, "친구 회원을 찾을 수 없습니다."),

    // 400 친구 요청 관련 오류
    FRIEND_STATUS_UNHANDLED(400, "처리할 수 없는 친구 요청 상태입니다."),

    // 400 요청자가 요청을 추가/수락/거절하려고 할 때,
    FRIEND_REQUEST_SELF(400, "자기 자신을 친구로 추가할 수 없습니다."),
    FRIEND_REQUEST_NOT_ALLOWED_ACCEPT(400, "요청한 사람이 친구 수락할 수 없습니다. 친구에게 요청 수락을 받으세요."),
    FRIEND_REQUEST_NOT_ALLOWED_REJECT(400, "요청한 사람이 친구 요청을 거절할 수 없습니다. 친구의 요청 수락/거절을 기다리세요."),

    // 409 친구 요청할 수 없는 상태
    FRIEND_ALREADY_REQUEST_PENDING(409, "이미 친구 요청을 보냈습니다.  상대방의 수락을 기다려주세요."),
    FRIEND_ALREADY_RESPOND_PENDING(409, "이미 친구 요청을 받았습니다. 수락 또는 거절해주세요"),
    FRIEND_ALREADY_ACCEPTED(409, "이미 친구입니다."),
    FRIEND_ALREADY_REJECTED(409, "이전에 거절한 친구 요청입니다. 다시 요청할 수 없습니다."),

    // 400 유효하지 않은 상태
    FRIEND_NOT_ACCEPTED(400, "친구 요청이 수락되지 않았습니다."),

    // 403 권한 관련 오류
    FRIEND_ACCESS_DENIED(403, "로그인한 회원과 관련된 친구가 아닙니다.");
}
