package com.back.domain.club.clubLink.entity

import com.back.domain.club.club.entity.Club
import jakarta.persistence.*
import jdk.jfr.Description
import java.time.LocalDateTime

@Entity
class ClubLink(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Description("초대 코드")
    @Column(unique = true, nullable = false, length = 50)
    var inviteCode: String,

    @Description("링크 생성 날짜")
    @Column(columnDefinition = "TIMESTAMP")
    var createdAt: LocalDateTime,

    @Description("링크 만료 날짜")
    @Column(columnDefinition = "TIMESTAMP")
    var expiresAt: LocalDateTime,

    @Description("클럽 정보")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_id", nullable = false)
    var club: Club
) {

    fun isExpired(): Boolean {
        return expiresAt.isBefore(LocalDateTime.now())
    }

    companion object {
        fun builder(
            inviteCode: String,
            createdAt: LocalDateTime,
            expiresAt: LocalDateTime,
            club: Club
        ): ClubLink {
            return ClubLink(
                inviteCode = inviteCode,
                createdAt = createdAt,
                expiresAt = expiresAt,
                club = club
            )
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ClubLink
        return id != null && id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
