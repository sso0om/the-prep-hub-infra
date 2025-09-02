package com.back.domain.schedule.schedule.entity

import com.back.domain.checkList.checkList.entity.CheckList
import com.back.domain.club.club.entity.Club
import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.hibernate.Hibernate
import java.time.LocalDateTime

@Entity
class Schedule(
    var title: String,
    var content: String,
    var startDate: LocalDateTime,
    var endDate: LocalDateTime,
    var spot: String,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    val club: Club,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        private set

    var isActive: Boolean = true
        private set

    @OneToOne(mappedBy = "schedule", fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JsonIgnore
    var checkList: CheckList? = null
        private set

    init {
        club.addClubSchedule(this)
    }

    fun updateCheckList(checkList: CheckList?) {
        this.checkList = checkList
    }

    fun modify(title: String, content: String, startDate: LocalDateTime, endDate: LocalDateTime, spot: String) {
        this.title = title
        this.content = content
        this.startDate = startDate
        this.endDate = endDate
        this.spot = spot
    }

    fun deactivate() {
        isActive = false
    }

    fun canDelete(): Boolean {
        return checkList == null || checkList?.isActive == false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as Schedule

        return this.id != null && this.id == other.id
    }

    override fun hashCode(): Int = javaClass.hashCode()
}
