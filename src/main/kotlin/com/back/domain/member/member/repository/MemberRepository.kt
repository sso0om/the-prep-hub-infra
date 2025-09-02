package com.back.domain.member.member.repository

import com.back.domain.member.member.entity.Member
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    fun findByNickname(nickname: String): Member?

    fun existsByNicknameAndTag(nickname: String, tag: String): Boolean

    fun findByNicknameAndTag(nickname: String, tag: String): Member?

    // 회원 ID로 회원 정보를 조회하며, 친구 관계를 포함 (n+1 쿼리 방지)
    @EntityGraph(attributePaths = [
        "friendshipsAsMember1.member2.memberInfo", // member1로 등록 된 경우 친구 member2의 정보
        "friendshipsAsMember2.member1.memberInfo"  // member2로 등록 된 경우 친구 member1의 정보
    ])
    @Query("SELECT m FROM Member m WHERE m.id = :memberId")
    fun findWithFriendsById(memberId: Long): Member?

    // clubId로 조회된 클럽의 GUEST 중 입력된 닉네임을 가지고 있는 GUEST가 있는지 조회
    @Query("""
        select case when count(m) > 0 then true else false end
        from Member m
        join m.clubMembers cm
        where m.nickname = :nickname
          and m.memberType = 'GUEST'
          and cm.club.id = :clubId
    """)
    fun existsGuestNicknameInClub(@Param("nickname") nickname: String, @Param("clubId") clubId: Long): Boolean

    // clubId로 조회된 클럽의 GUEST 중 입력된 닉네임을 가지고 있는 GUEST를 반환
    @Query("""
        select m
        from Member m
        join m.clubMembers cm
        where m.nickname = :nickname
          and m.memberType = com.back.global.enums.MemberType.GUEST
          and cm.club.id = :clubId
    """)
    fun findByGuestNicknameInClub(@Param("nickname") nickname: String, @Param("clubId") clubId: Long): Member?

    fun findByMemberInfo_Email(email: String): Member?
}
