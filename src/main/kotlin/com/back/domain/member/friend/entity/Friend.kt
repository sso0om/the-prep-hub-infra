package com.back.domain.member.friend.entity

import com.back.domain.member.member.entity.Member
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@EntityListeners(AuditingEntityListener::class)
class Friend(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val requestedBy: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val member1: Member,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val member2: Member,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: FriendStatus = FriendStatus.PENDING
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @CreatedDate
    @Column(nullable = false, updatable = false)
    lateinit var createdDate: LocalDateTime
        private set

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Friend) return false

        if (id == 0L || other.id == 0L) return false
        return this.id != null && this.id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    // ===== 유틸 메서드 =====
    /**
     * 친구 관계가 member1, member2 중 하나라도 포함되어 있는지 확인합니다.
     * @param member 확인할 멤버
     * @return 포함되어 있으면 true, 아니면 false
     */
    fun involves(member: Member): Boolean {
        return member1 == member || member2 == member
    }

    /**
     * 이 친구 관계에 포함된 다른 멤버를 반환합니다.
     * @param self 자신을 나타내는 멤버
     * @return 다른 멤버
     * @throws IllegalArgumentException 만약 self가 이 친구 관계에 포함되지 않는 경우
     */
    fun getOther(self: Member): Member {
        return when (self) {
            member1 -> member2
            member2 -> member1
            else -> throw IllegalArgumentException("해당 멤버는 이 친구 관계에 포함되지 않습니다.")
        }
    }
}
