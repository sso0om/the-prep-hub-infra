package com.back.domain.preset.preset.entity

import com.back.global.enums.CheckListItemCategory
import jakarta.persistence.*
import org.hibernate.Hibernate

/**
 * 중요: 이 엔티티 클래스는 JPA가 기본 생성자를 만들 수 있도록
 * build.gradle.kts에 'kotlin-jpa' 플러그인이 설정되어야 합니다.
 */
@Entity
class PresetItem(
    @Column(nullable = false)
    val content: String, // 내용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val category: CheckListItemCategory, // 카테고리

    @Column(nullable = false)
    val sequence: Int, // 정렬 순서
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set // 외부에서는 id를 변경할 수 없도록 private set으로 설정

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "preset_id", nullable = true) // 실제 컬럼명에 맞추어 수정
    var preset: Preset? = null // 프리셋 (양방향 연관관계)

    // id 기반으로 엔티티의 동등성을 비교합니다.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as PresetItem
        return id != null && id == other.id
    }

    // id 기반으로 해시코드를 생성합니다.
    override fun hashCode(): Int = javaClass.hashCode()
}