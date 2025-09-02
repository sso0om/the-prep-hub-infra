package com.back.domain.checkList.itemAssign.repository

import com.back.domain.checkList.itemAssign.entity.ItemAssign
import org.springframework.data.jpa.repository.JpaRepository

interface ItemAssignRepository : JpaRepository<ItemAssign, Long> {

}