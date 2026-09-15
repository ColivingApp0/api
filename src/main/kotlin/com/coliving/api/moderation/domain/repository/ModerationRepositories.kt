package com.coliving.api.moderation.domain.repository

import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.model.SupportCase
import java.util.UUID

interface SupportCaseRepository {

    fun findById(id: UUID): SupportCase?

    /** Support queue, newest first; null filters are ignored. */
    fun search(
        type: CaseType?,
        status: CaseStatus?,
        priority: CasePriority?,
        assigneeUserId: UUID?,
        onlyOpen: Boolean,
    ): List<SupportCase>

    /** Cases where the user is the reporter/opener (their own follow-up). */
    fun findByOpenedBy(openedByUserId: UUID): List<SupportCase>

    fun save(case: SupportCase)
}

interface CaseEventRepository {

    /** Traceability of a case in chronological order. */
    fun findByCase(caseId: UUID): List<CaseEvent>

    fun save(event: CaseEvent)
}