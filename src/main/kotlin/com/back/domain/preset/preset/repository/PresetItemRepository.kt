package com.back.domain.preset.preset.repository

import com.back.domain.preset.preset.entity.PresetItem
import org.springframework.data.jpa.repository.JpaRepository

interface PresetItemRepository : JpaRepository<PresetItem, Long>
