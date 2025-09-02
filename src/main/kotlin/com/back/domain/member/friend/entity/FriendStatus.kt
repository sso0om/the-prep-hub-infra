package com.back.domain.member.friend.entity

enum class FriendStatus(val description: String) {
    PENDING("요청 중"),  // 요청 중
    ACCEPTED("수락됨"),  // 수락됨
    REJECTED("거절됨"); // 거절됨
}
