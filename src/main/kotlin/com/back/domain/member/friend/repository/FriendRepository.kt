package com.back.domain.member.friend.repository

import com.back.domain.member.friend.entity.Friend
import com.back.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface FriendRepository : JpaRepository<Friend, Long> {
    /**
     * 친구 관계를 ID로 조회할 때, Member 엔티티의 memberInfo를 함께 로딩 (n+1 쿼리 방지)
     * @param friendId 친구 ID
     * @return Friend 엔티티와 관련된 Member 정보 포함
     */
    @EntityGraph(attributePaths = ["member1.memberInfo", "member2.memberInfo"])
    override fun findById(friendId: Long): Optional<Friend>

    /**
     * 두 멤버 간의 친구 관계 조회
     * @param requester 요청자 멤버
     * @param responder 응답자 멤버
     * @return 두 멤버 간의 친구 관계가 존재하는 경우 Friend 엔티티
     */
    @Query(
        """
            SELECT f FROM Friend f 
            WHERE (f.member1 = :requester AND f.member2 = :responder) 
            OR (f.member1 = :responder AND f.member2 = :requester)
            
            """
    )
    fun findByMembers(
        @Param("requester") requester: Member,
        @Param("responder") responder: Member
    ): Friend?

    /**
     * 요청자가 보낸 친구 요청 중 가장 최근의 친구 관계 조회
     * @param requestedBy 요청자 멤버
     * @return 가장 최근의 Friend 엔티티
     */
    fun findFirstByRequestedByOrderByIdDesc(requestedBy: Member): Friend?
}
