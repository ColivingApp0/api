package com.coliving.api.moderation.application.usecase

import com.coliving.api.moderation.application.port.out.CaseIntake
import com.coliving.api.moderation.domain.enums.CaseEventType
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.model.SupportCase
import com.coliving.api.moderation.domain.repository.CaseEventRepository
import com.coliving.api.moderation.domain.repository.SupportCaseRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Opens cases on behalf of the reporting contexts (RF-052, RF-072) and records
 * the opening entry of the traceability chain. This is the only way another
 * context can raise a case, so the intake rules (mandatory description, no
 * self-report) live here and are impossible to bypass.
 */
@Service
class CaseIntakeService(
    private val caseRepository: SupportCaseRepository,
    private val caseEventRepository: CaseEventRepository,
) : CaseIntake {

    @Transactional
    override fun openCase(
        type: CaseType,
        subjectType: CaseSubjectType,
        subjectId: UUID,
        reportedUserId: UUID?,
        openedByUserId: UUID?,
        description: String,
        priority: CasePriority,
    ): UUID {
        val now = Instant.now()
        val case = SupportCase.open(
            id = UUID.randomUUID(),
            type = type,
            subjectType = subjectType,
            subjectId = subjectId,
            reportedUserId = reportedUserId,
            openedByUserId = openedByUserId,
            description = description,
            priority = priority,
            now = now,
        )
        caseRepository.save(case)
        caseEventRepository.save(
            CaseEvent.record(
                id = UUID.randomUUID(),
                caseId = case.id,
                type = CaseEventType.ABIERTO,
                actorUserId = openedByUserId ?: SYSTEM_ACTOR,
                note = case.description,
                now = now,
            ),
        )
        return case.id
    }

    companion object {
        /** Actor used when a case is raised without an authenticated user. */
        val SYSTEM_ACTOR: UUID = UUID(0, 0)
    }
}