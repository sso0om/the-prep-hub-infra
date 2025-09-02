package com.back.domain.club.club.repository

import com.back.domain.club.club.entity.Club
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface ClubRepository : JpaRepository<Club, Long>, JpaSpecificationExecutor<Club> {
    /**
     * 마지막으로 생성된 클럽을 반환합니다.
     * @return 마지막으로 생성된 클럽
     */
    fun findFirstByOrderByIdDesc(): Club?

    fun findAllByIsPublicTrue(pageable: Pageable): Page<Club>

    fun findByIdAndStateIsTrue(clubId: Long): Club?

    @Query(
        """
            SELECT c FROM Club c 
            WHERE c.id = :clubId 
            AND c.state = TRUE 
            AND c.endDate >= CURRENT_DATE
            """
    )
    fun findValidAndActiveClub(
        @Param("clubId") clubId: Long
    ): Club?
}
