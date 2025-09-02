package com.back.domain.member.friend.service

import com.back.domain.member.friend.dto.FriendDto
import com.back.domain.member.friend.dto.FriendMemberDto
import com.back.domain.member.friend.dto.FriendStatusDto
import com.back.domain.member.friend.entity.Friend
import com.back.domain.member.friend.entity.FriendStatus
import com.back.domain.member.friend.error.FriendErrorCode
import com.back.domain.member.friend.repository.FriendRepository
import com.back.domain.member.member.entity.Member
import com.back.domain.member.member.entity.MemberInfo
import com.back.domain.member.member.error.MemberErrorCode
import com.back.domain.member.member.repository.MemberInfoRepository
import com.back.domain.member.member.repository.MemberRepository
import com.back.global.exception.ServiceException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class FriendService(
    private val memberRepository: MemberRepository,
    private val memberInfoRepository: MemberInfoRepository,
    private val friendRepository: FriendRepository
) {
    /**
     * 내 친구 목록을 조회하는 메서드
     * @param memberId 로그인 회원 아이디
     * @return List<FriendsResDto>
    </FriendsResDto> */
    fun getFriends(memberId: Long, statusFilter: FriendStatusDto?): List<FriendDto> {
        // 로그인 회원
        val member = memberRepository.findWithFriendsById(memberId)
            ?: throw ServiceException(MemberErrorCode.MEMBER_NOT_FOUND)

        // member1로 등록된 친구 관계 + member2로 등록된 친구 관계
        val allFriends = member.friendshipsAsMember1 + member.friendshipsAsMember2

        return allFriends.asSequence()
            .filter { friend -> matchesStatusFilter(friend, statusFilter, member) }
            .map { friend -> FriendDto.from(friend, friend.getOther(member)) }
            .sortedBy { it.friendNickname }
            .toList()
    }

    /**
     * 친구 엔티티를 아이디로 조회하는 메서드
     * @param friendId
     * @return Friend
     */
    fun getFriendById(friendId: Long): Friend {
        return friendRepository.findById(friendId)
            .orElseThrow { ServiceException(FriendErrorCode.FRIEND_NOT_FOUND) }
    }

    /**
     * 친구 추가 요청을 처리하는 메서드
     * @param memberId    로그인 회원 아이디
     * @param friendEmail 친구(Member) 이메일
     * @return FriendDto
     */
    @Transactional
    fun addFriend(memberId: Long, friendEmail: String): FriendDto {
        // 로그인 회원(친구 요청을 보낸 회원)
        val requester = getMember(memberId)

        // 친구 요청을 받는 회원
        val responder = getFriendMemberInfoByEmail(friendEmail).getMember()
            ?: throw ServiceException(MemberErrorCode.MEMBER_NOT_FOUND)

        // 자기 자신을 친구로 추가하는 경우 예외 처리
        if (requester == responder) {
            throw ServiceException(FriendErrorCode.FRIEND_REQUEST_SELF)
        }

        // 이미 친구인 경우 예외 처리
        validateForAddFriend(requester, responder)

        // id 순서로 member1, member2 지정
        val (lowerMember, higherMember) = if (requester.id!! < responder.id!!) {
            requester to responder
        } else {
            responder to requester
        }

        // 친구 요청 생성
        val friend = Friend(
            requestedBy = requester,
            member1 = lowerMember,
            member2 = higherMember,
            status = FriendStatus.PENDING
        )

        // 친구 요청 저장
        friendRepository.save(friend)

        return FriendDto.from(friend, responder)
    }

    /**
     * 친구 요청을 수락하는 메서드
     * @param memberId 로그인 회원 아이디
     * @param friendId 친구 엔티티 아이디
     * @return FriendDto
     */
    @Transactional
    fun acceptFriend(memberId: Long, friendId: Long): FriendDto {
        // 로그인 회원(친구 요청을 받은 회원)
        val me = getMember(memberId)

        // 친구 엔티티
        val friend = getFriendById(friendId)

        // 요청자가 친구 요청을 수락 / 상태가 PENDING이 아닌 경우 예외 처리
        validateForAcceptance(friend, me)

        // 친구 요청 수락
        friend.status = FriendStatus.ACCEPTED

        return FriendDto.from(friend, friend.getOther(me))
    }

    /**
     * 친구 요청을 거절하는 메서드
     * @param memberId 로그인 회원 아이디
     * @param friendId 친구 엔티티 아이디
     * @return FriendDto
     */
    @Transactional
    fun rejectFriend(memberId: Long, friendId: Long): FriendDto {
        // 로그인 회원(친구 요청을 받은 회원)
        val me = getMember(memberId)

        // 친구 엔티티
        val friend = getFriendById(friendId)

        // 요청자가 친구 요청을 거절 / 이미 친구인 경우 예외 처리
        validateForRejection(friend, me)

        // 친구 요청 거절
        friend.status = FriendStatus.REJECTED

        return FriendDto.from(friend, friend.getOther(me))
    }

    /**
     * 친구 삭제를 처리하는 메서드
     * @param memberId 로그인 회원 아이디
     * @param friendId 친구 엔티티 아이디
     * @return FriendDelDto
     */
    @Transactional
    fun deleteFriend(memberId: Long, friendId: Long): FriendMemberDto {
        // 로그인 회원
        val me = getMember(memberId)

        // 친구 엔티티
        val friend = getFriendById(friendId)

        // 친구 요청의 상태가 ACCEPTED가 아닌 경우 예외 처리
        validateForDelete(friend, me)

        // 친구 삭제
        friendRepository.delete(friend)

        return FriendMemberDto.from(friend.getOther(me))
    }


    // ========== Helper Methods ==========

    /**
     * 회원 정보를 가져오는 메서드
     * @param memberId 로그인 회원 아이디
     * @return Member
     */
    private fun getMember(memberId: Long): Member {
        return memberRepository.findById(memberId)
            .orElseThrow { ServiceException(MemberErrorCode.MEMBER_NOT_FOUND) }
    }

    /**
     * 친구 회원 정보를 이메일로 가져오는 메서드
     * @param friendEmail
     * @return MemberInfo
     */
    private fun getFriendMemberInfoByEmail(friendEmail: String): MemberInfo {
        return memberInfoRepository.findByEmailWithMember(friendEmail)
            ?: throw ServiceException(MemberErrorCode.MEMBER_NOT_FOUND)
    }

    private fun matchesStatusFilter(friend: Friend, statusFilter: FriendStatusDto?, me: Member): Boolean {
        return when (statusFilter) {
            null, FriendStatusDto.ALL -> true
            FriendStatusDto.ACCEPTED -> friend.status == FriendStatus.ACCEPTED
            FriendStatusDto.SENT -> friend.status == FriendStatus.PENDING && friend.requestedBy == me
            FriendStatusDto.RECEIVED -> friend.status == FriendStatus.PENDING && friend.requestedBy != me
            FriendStatusDto.REJECTED -> friend.status == FriendStatus.REJECTED
        }
    }


    // ========== Validation Methods ==========

    /**
     * 사용자의 친구 관계인지 확인
     * @param friend 친구 엔티티
     * @param me 로그인 회원
     */
    private fun validateFriend(friend: Friend, me: Member) {
        if (!friend.involves(me)) {
            throw ServiceException(FriendErrorCode.FRIEND_ACCESS_DENIED)
        }
    }

    /**
     * 이미 친구인 경우 예외 처리
     * @param friend 친구 엔티티
     */
    private fun validatePending(friend: Friend) {
        when (friend.status) {
            FriendStatus.PENDING -> return
            FriendStatus.ACCEPTED -> throw ServiceException(FriendErrorCode.FRIEND_ALREADY_ACCEPTED)
            FriendStatus.REJECTED -> throw ServiceException(FriendErrorCode.FRIEND_ALREADY_REJECTED)
        }
    }

    /**
     * 친구 추가 시 유효성 검사
     * (이미 친구인 경우 예외 처리 )
     * @param requester 요청자
     * @param responder 응답자
     */
    private fun validateForAddFriend(requester: Member, responder: Member) {
        friendRepository.findByMembers(requester, responder)?.let { existingFriend ->
            // 친구 관계 상태에 따라 에러 메시지
            val errorCode = when (existingFriend.status) {
                FriendStatus.PENDING -> {
                    // 요청자 여부에 따라 에러 메시지
                    if (existingFriend.requestedBy == requester) {
                        FriendErrorCode.FRIEND_ALREADY_REQUEST_PENDING
                    } else {
                        FriendErrorCode.FRIEND_ALREADY_RESPOND_PENDING
                    }
                }
                FriendStatus.ACCEPTED -> FriendErrorCode.FRIEND_ALREADY_ACCEPTED
                FriendStatus.REJECTED -> FriendErrorCode.FRIEND_ALREADY_REJECTED
            }
            throw ServiceException(errorCode)
        }
    }

    /**
     * 친구 수락 시 유효성 검사
     * (요청자가 친구 요청을 수락 / 상태가 PENDING이 아닌 경우 예외 처리)
     * @param me 로그인 회원
     * @param friend 친구 엔티티
     */
    private fun validateForAcceptance(friend: Friend, me: Member) {
        // 사용자의 친구 관계인지 확인
        validateFriend(friend, me)

        // 받는이가 아닌 요청자가 친구 요청을 수락하는 경우 예외 처리
        if (me == friend.requestedBy) {
            throw ServiceException(FriendErrorCode.FRIEND_REQUEST_NOT_ALLOWED_ACCEPT)
        }

        // PENDING 이외 상태는 수락 불가
        validatePending(friend)
    }

    /**
     * 친구 거절 시 유효성 검사
     * (요청자가 친구 요청을 수락 / 이미 친구인 경우 예외 처리)
     * @param me 로그인 회원
     * @param friend 친구 엔티티
     */
    private fun validateForRejection(friend: Friend, me: Member) {
        // 사용자의 친구 관계인지 확인
        validateFriend(friend, me)

        // 받는이가 아닌 요청자가 친구 요청을 거절하는 경우 예외 처리
        if (me == friend.requestedBy) {
            throw ServiceException(FriendErrorCode.FRIEND_REQUEST_NOT_ALLOWED_REJECT)
        }

        // PENDING 이외 상태는 거절 불가
        validatePending(friend)
    }

    /**
     * 친구 삭제 시 유효성 검사
     * (친구 요청의 상태가 ACCEPTED가 아닌 경우 예외 처리)
     * @param me 로그인 회원
     * @param friend 친구 엔티티
     */
    private fun validateForDelete(friend: Friend, me: Member) {
        // 사용자의 친구 관계인지 확인
        validateFriend(friend, me)

        // 친구 요청의 상태가 ACCEPTED가 아닌 경우 예외 처리
        if (friend.status != FriendStatus.ACCEPTED) {
            throw ServiceException(FriendErrorCode.FRIEND_NOT_ACCEPTED)
        }
    }
}
