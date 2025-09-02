package com.back.domain.checkList.checkList.entity

import com.back.domain.checkList.itemAssign.entity.ItemAssign
import com.back.global.enums.CheckListItemCategory
import jakarta.persistence.*
import org.hibernate.Hibernate

/**
 * 중요: 이 엔티티 클래스는 JPA가 기본 생성자를 만들 수 있도록
 * build.gradle.kts에 'kotlin-jpa' 플러그인이 설정되어야 합니다.
 */
@Entity
class CheckListItem(
    @Column(nullable = false)
    val content: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: CheckListItemCategory,

    @Column(nullable = false)
    val sequence: Int,

    var isChecked: Boolean = false,

    itemAssigns: List<ItemAssign> = emptyList()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "check_list_id", nullable = false)
    lateinit var checkList: CheckList // 1. lateinit으로 안전한 관계 설정

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "checkListItem", cascade = [CascadeType.ALL], orphanRemoval = true)
    val itemAssigns: MutableList<ItemAssign> = mutableListOf()

    init {
        // 2. 생성 시점에 양방향 연관관계를 설정합니다.
        itemAssigns.forEach { addItemAssign(it) }
    }

    fun updateIsChecked(isChecked: Boolean) {
        this.isChecked = isChecked
    }

    // 연관관계 편의 메서드
    private fun addItemAssign(itemAssign: ItemAssign) {
        this.itemAssigns.add(itemAssign)
        itemAssign.checkListItem = this
    }

    // 3. id 기반으로 엔티티의 동등성을 비교합니다.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as CheckListItem
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}