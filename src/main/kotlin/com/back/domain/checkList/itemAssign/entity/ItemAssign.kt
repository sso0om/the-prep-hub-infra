package com.back.domain.checkList.itemAssign.entity

import com.back.domain.checkList.checkList.entity.CheckListItem
import com.back.domain.club.clubMember.entity.ClubMember
import jakarta.persistence.*
import org.hibernate.Hibernate

/**
 * 멤버를 checklistItem에 할당하는 엔티티
 * 중요: 이 엔티티 클래스는 JPA가 기본 생성자를 만들 수 있도록
 * build.gradle.kts에 'kotlin-jpa' 플러그인이 설정되어야 합니다.
 */
@Entity
@Table(
    uniqueConstraints = [UniqueConstraint(columnNames = ["club_member_id", "check_list_item_id"])]
)
class ItemAssign(
    /**
     * 할당된 인원
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "club_member_id", nullable = false)
    var clubMember: ClubMember,

    /**
     * 체크 여부
     */
    var isChecked: Boolean = false
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    /**
     * 할당된 체크리스트 아이템
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_list_item_id", nullable = false)
    lateinit var checkListItem: CheckListItem

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as ItemAssign
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}