package com.back.domain.preset.preset.repository

import com.back.domain.member.member.entity.Member
import com.back.domain.preset.preset.entity.Preset
import org.springframework.data.jpa.repository.JpaRepository

interface PresetRepository : JpaRepository<Preset, Long> {
    fun findByOwner(member: Member): List<Preset>
}