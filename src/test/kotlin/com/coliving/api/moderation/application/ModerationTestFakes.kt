package com.coliving.api.moderation.application

import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.model.SupportCase
import com.coliving.api.moderation.domain.repository.CaseEventRepository
import com.coliving.api.moderation.domain.repository.SupportCaseRepository
import java.util.UUID

class FakeSupportCaseRepository : SupportCaseRepository {
    val store = mutableMapOf<UUID, SupportCase>()

    override fun findById(id: UUID): SupportCase? = store[id]

    override fun search(
        type: com.coliving.api.moderation.domain.enums.CaseType?,
        status: com.coliving.api.moderation.domain.enums.CaseStatus?,
        priority: com.coliving.api.moderation.domain.enums.CasePriority?,
        assigneeUserId: UUID?,
        onlyOpen: Boolean,
    ): List<SupportCase> = store.values
        .filter { type == null || it.type == type }
        .filter { status == null || it.status == status }
        .filter { priority == null || it.priority == priority }
        .filter { assigneeUserId == null || it.assigneeUserId == assigneeUserId }
        .filter { !onlyOpen || it.isOpen() }
        .sortedByDescending { it.openedAt }

    override fun findByOpenedBy(openedByUserId: UUID): List<SupportCase> =
        store.values.filter { it.openedByUserId == openedByUserId }.sortedByDescending { it.openedAt }

    override fun save(case: SupportCase) {
        store[case.id] = case
    }
}

class FakeCaseEventRepository : CaseEventRepository {
    val store = mutableListOf<CaseEvent>()

    override fun findByCase(caseId: UUID): List<CaseEvent> =
        store.filter { it.caseId == caseId }.sortedBy { it.occurredAt }

    override fun save(event: CaseEvent) {
        store.add(event)
    }
}