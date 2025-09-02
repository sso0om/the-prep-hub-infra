package com.back.domain.checkList.checkList.repository

import com.back.domain.checkList.checkList.entity.CheckListItem
import org.springframework.data.jpa.repository.JpaRepository

interface CheckListItemRepository : JpaRepository<CheckListItem, Long> {

}