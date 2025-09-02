package com.back.domain.checkList.checkList.entity

import com.back.domain.schedule.schedule.entity.Schedule
import jakarta.persistence.*
import org.hibernate.Hibernate

/**
 * 중요: 이 엔티티 클래스는 JPA가 기본 생성자를 만들 수 있도록
 * build.gradle.kts에 'kotlin-jpa' 플러그인이 설정되어야 합니다.
 */
@Entity
class CheckList(
    var isActive: Boolean,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    var schedule: Schedule,

    checkListItems: List<CheckListItem> = emptyList()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], mappedBy = "checkList", orphanRemoval = true)
    val checkListItems: MutableList<CheckListItem> = mutableListOf()

    init {
        // 생성 시점에 checkListItems를 초기화하고, 양방향 연관관계를 설정합니다.
        checkListItems.forEach { addCheckListItem(it) }
        // Schedule과의 양방향 연관관계도 설정합니다.
        schedule.updateCheckList(this)
    }

    fun deactivate() {
        this.isActive = false
    }

    fun updateIsActive(isActive: Boolean) {
        this.isActive = isActive
    }

    fun updateCheckListItems(newItems: List<CheckListItem>) {
        // 기존 아이템을 모두 제거하고 새로운 아이템으로 교체합니다.
        this.checkListItems.clear()
        newItems.forEach { addCheckListItem(it) }
    }

    /**
     * 연관관계 편의 메서드
     */
    private fun addCheckListItem(item: CheckListItem) {
        this.checkListItems.add(item)
        item.checkList = this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as CheckList
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}