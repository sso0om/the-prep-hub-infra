package com.back.domain.club.club.entity

import com.back.domain.club.clubMember.entity.ClubMember
import com.back.domain.schedule.schedule.entity.Schedule
import com.back.global.enums.ClubCategory
import com.back.global.enums.EventType
import jakarta.persistence.*
import java.time.LocalDate

@Entity

class Club(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(length = 50, nullable = false)
    var name: String,

    @Column(columnDefinition = "TEXT")
    var bio: String? = null,

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    var category: ClubCategory,

    @Column(length = 256, nullable = false)
    var mainSpot: String,

    @Column(nullable = false)
    var maximumCapacity: Int,

    @Column(nullable = false)
    var recruitingStatus: Boolean = true,

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    var eventType: EventType,

    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,

    @Column(length = 256)
    var imageUrl: String? = null,

    @Column(nullable = false)
    var isPublic: Boolean = true,

    var leaderId: Long? = null,
    @Column(nullable = false)
    var state: Boolean = true
) {

    @OneToMany(mappedBy = "club", cascade = [CascadeType.ALL], orphanRemoval = true)
    val clubMembers: MutableList<ClubMember> = mutableListOf()

    @OneToMany(mappedBy = "club", cascade = [CascadeType.ALL], orphanRemoval = true)
    val clubSchedules: MutableList<Schedule> = mutableListOf()

    // ---------------- 메서드 ----------------
    fun changeState(state: Boolean) {
        this.state = state
    }

    fun changeRecruitingStatus(recruitingStatus: Boolean) {
        this.recruitingStatus = recruitingStatus
    }

    fun addClubMember(clubMember: ClubMember) {
        clubMembers.add(clubMember)
        clubMember.club = this
    }

    fun removeClubMember(clubMember: ClubMember) {
        clubMembers.remove(clubMember)
        clubMember.club = null
    }

    fun addClubSchedule(schedule: Schedule) {
        clubSchedules.add(schedule)
    }

    fun updateImageUrl(imageUrl: String?) {
        this.imageUrl = imageUrl
    }

    fun updateInfo(
        name: String,
        bio: String?,
        category: ClubCategory,
        mainSpot: String,
        maximumCapacity: Int,
        recruitingStatus: Boolean,
        eventType: EventType,
        startDate: LocalDate?,
        endDate: LocalDate?,
        isPublic: Boolean
    ) {
        this.name = name
        this.bio = bio
        this.category = category
        this.mainSpot = mainSpot
        this.maximumCapacity = maximumCapacity
        this.recruitingStatus = recruitingStatus
        this.eventType = eventType
        this.startDate = startDate
        this.endDate = endDate
        this.isPublic = isPublic
    }

    // ---------------- equals & hashCode (ID 기준) ----------------
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Club) return false
        if (id == null || other.id == null) return false
        return id == other.id
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0

    // --- builder(자바 호환성) ---
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
    class Builder {
        private var name: String = ""
        private var bio: String? = null
        private var category: ClubCategory = ClubCategory.SPORTS
        private var mainSpot: String = ""
        private var maximumCapacity: Int = 0
        private var recruitingStatus: Boolean = true
        private var eventType: EventType = EventType.ONE_TIME
        private var startDate: LocalDate? = null
        private var endDate: LocalDate? = null
        private var imageUrl: String? = null
        private var isPublic: Boolean = true
        private var leaderId: Long? = null
        private var state: Boolean = true

        fun name(name: String) = apply { this.name = name }
        fun bio(bio: String?) = apply { this.bio = bio }
        fun category(category: ClubCategory) = apply { this.category = category }
        fun mainSpot(mainSpot: String) = apply { this.mainSpot = mainSpot }
        fun maximumCapacity(capacity: Int) = apply { this.maximumCapacity = capacity }
        fun recruitingStatus(recruitingStatus: Boolean) = apply { this.recruitingStatus = recruitingStatus }
        fun eventType(eventType: EventType) = apply { this.eventType = eventType }
        fun startDate(startDate: LocalDate?) = apply { this.startDate = startDate }
        fun endDate(endDate: LocalDate?) = apply { this.endDate = endDate }
        fun imageUrl(imageUrl: String?) = apply { this.imageUrl = imageUrl }
        fun isPublic(isPublic: Boolean) = apply { this.isPublic = isPublic }
        fun leaderId(leaderId: Long?) = apply { this.leaderId = leaderId }
        fun state(state: Boolean) = apply { this.state = state }

        fun build() = Club(
            name = name,
            bio = bio,
            category = category,
            mainSpot = mainSpot,
            maximumCapacity = maximumCapacity,
            recruitingStatus = recruitingStatus,
            eventType = eventType,
            startDate = startDate,
            endDate = endDate,
            imageUrl = imageUrl,
            isPublic = isPublic,
            leaderId = leaderId,
            state = state
        )
    }
}
