package com.back.domain.member.friend.dto

enum class FriendStatusDto {
    SENT,  // 내가 보낸 요청
    RECEIVED,  // 내가 받은 요청
    ACCEPTED,  // 수락됨(친구)
    REJECTED,  // 거절됨
    ALL
}