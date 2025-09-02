package com.back.domain.member.member.repository

import com.back.domain.member.member.entity.MemberInfo
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface MemberInfoRepository : JpaRepository<MemberInfo, Long> {

    fun findByEmail(email: String): MemberInfo?

    fun findByApiKey(apiKey: String): MemberInfo?

    /**
     * 이메일로 회원 정보 조회 (MemberInfo와 Member를 함께 조회)
     * n+1 방지 fetch join
     */
    @Query("""
        SELECT mi FROM MemberInfo mi
        JOIN FETCH mi.member m
        WHERE mi.email = :email
    """)
    fun findByEmailWithMember(email: String): MemberInfo?
}