package com.back.domain.checkList.checkList.repository

import com.back.domain.checkList.checkList.entity.CheckList
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface CheckListRepository : JpaRepository<CheckList, Long> {

    // 활성화된 체크리스트 조회
    @Query("""
            SELECT c FROM CheckList c
            JOIN FETCH c.schedule
            WHERE c.id = :checkListId
            AND c.isActive = true
            """)
    fun findActiveCheckListById(checkListId: Long): Optional<CheckList>
}