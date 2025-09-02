package com.back.domain.preset.preset.entity

import com.back.domain.member.member.entity.Member
import jakarta.persistence.*
import org.hibernate.Hibernate

/**
 * 중요: 이 엔티티 클래스는 JPA가 기본 생성자를 만들 수 있도록
 * build.gradle.kts에 'kotlin-jpa' 플러그인이 설정되어야 합니다.
 */
@Entity
class Preset(
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var owner: Member,

    presetItems: List<PresetItem> = emptyList()
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    @OneToMany(fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true, mappedBy = "preset")
    val presetItems: MutableList<PresetItem> = mutableListOf()

    init {
        // 생성 시점에 presetItems를 초기화하고, 양방향 연관관계를 설정합니다.
        presetItems.forEach { addPresetItem(it) }
    }

    fun updateName(name: String) {
        this.name = name
    }

    fun updatePresetItems(newPresetItems: List<PresetItem>) {
        // 1) 기존 아이템들과의 연관관계 해제
        this.presetItems.toList().forEach { removePresetItem(it) }
        // 새로운 아이템들을 추가하면서 양방향 연관관계를 설정합니다.
        newPresetItems.forEach { addPresetItem(it) }
    }

    // 연관관계 편의 메서드
    private fun addPresetItem(presetItem: PresetItem) {
        this.presetItems.add(presetItem)
        presetItem.preset = this
    }

    private fun removePresetItem(presetItem: PresetItem) {
        this.presetItems.remove(presetItem)
        presetItem.preset = null
    }

    // id 기반으로 엔티티의 동등성을 비교합니다.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Preset
        return id != null && id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}