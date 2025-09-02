package com.back.domain.club.clubLink.repository

import com.back.domain.club.club.entity.Club
import com.back.domain.club.clubLink.entity.ClubLink
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.*

@Repository
interface ClubLinkRepository : JpaRepository<ClubLink, Long> {
    override fun findAll(): List<ClubLink>

    fun findByClubAndExpiresAtAfter(club: Club, expiresAt: LocalDateTime): ClubLink?

    fun findByInviteCode(inviteCode: String): ClubLink?
}
