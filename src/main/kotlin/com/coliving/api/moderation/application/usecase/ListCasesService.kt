package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.dto.CaseSearchQuery
import com.coliving.api.moderation.application.dto.CaseView
import com.coliving.api.moderation.domain.repository.SupportCaseRepository
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Support queue (RF-082) with the filters the team needs: type, status,
 * priority, assignee and "only open". A user can also follow up on the cases
 * they opened, without seeing anyone else's.
 */
@Service
class ListCasesService(
    private val caseRepository: SupportCaseRepository,
) {

    @Transactional(readOnly = true)
    fun search(query: CaseSearchQuery): List<CaseView> =
        caseRepository.search(
            type = query.type,
            status = query.status,
            priority = query.priority,
            assigneeUserId = query.assigneeUserId,
            onlyOpen = query.onlyOpen,
        ).map { it.toView() }

    @Transactional(readOnly = true)
    fun listOpenedBy(userId: UUID): List<CaseView> =
        caseRepository.findByOpenedBy(userId).map { it.toView() }
}