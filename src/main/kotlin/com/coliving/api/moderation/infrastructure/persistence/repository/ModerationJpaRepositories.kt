package com.coliving.api.moderation.infrastructure.persistence.repository

import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.moderation.infrastructure.persistence.entity.CaseEventEntity
import com.coliving.api.moderation.infrastructure.persistence.entity.SupportCaseEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SupportCaseJpaRepository : JpaRepository<SupportCaseEntity, UUID> {

    /**
     * Support queue with optional filters. The query keeps every filter optional
     * (null = ignore) so the same method serves the whole queue and the
     * narrowed searches of the support team.
     */
    @Query(
        """
        SELECT c FROM SupportCaseEntity c
        WHERE (:type IS NULL OR c.type = :type)
          AND (:status IS NULL OR c.status = :status)
          AND (:priority IS NULL OR c.priority = :priority)
          AND (:assigneeUserId IS NULL OR c.assigneeUserId = :assigneeUserId)
          AND (:onlyOpen = FALSE OR c.status IN (com.coliving.api.moderation.domain.enums.CaseStatus.ABIERTO,
                                                 com.coliving.api.moderation.domain.enums.CaseStatus.EN_REVISION))
        ORDER BY c.openedAt DESC
        """,
    )
    fun search(
        @Param("type") type: CaseType?,
        @Param("status") status: CaseStatus?,
        @Param("priority") priority: CasePriority?,
        @Param("assigneeUserId") assigneeUserId: UUID?,
        @Param("onlyOpen") onlyOpen: Boolean,
    ): List<SupportCaseEntity>

    fun findByOpenedByUserIdOrderByOpenedAtDesc(openedByUserId: UUID): List<SupportCaseEntity>
}

interface CaseEventJpaRepository : JpaRepository<CaseEventEntity, UUID> {

    /** Traceability in chronological order (RF-082). */
    fun findByCaseIdOrderByOccurredAtAsc(caseId: UUID): List<CaseEventEntity>
}