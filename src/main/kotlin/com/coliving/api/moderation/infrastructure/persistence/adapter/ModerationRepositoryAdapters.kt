package com.coliving.api.moderation.infrastructure.persistence.adapter

import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseType
import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.model.SupportCase
import com.coliving.api.moderation.domain.repository.CaseEventRepository
import com.coliving.api.moderation.domain.repository.SupportCaseRepository
import com.coliving.api.moderation.infrastructure.persistence.mapper.ModerationMappers
import com.coliving.api.moderation.infrastructure.persistence.repository.CaseEventJpaRepository
import com.coliving.api.moderation.infrastructure.persistence.repository.SupportCaseJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class SupportCaseRepositoryAdapter(
    private val jpaRepository: SupportCaseJpaRepository,
) : SupportCaseRepository {

    override fun findById(id: UUID): SupportCase? =
        jpaRepository.findById(id).map(ModerationMappers::toDomain).orElse(null)

    override fun search(
        type: CaseType?,
        status: CaseStatus?,
        priority: CasePriority?,
        assigneeUserId: UUID?,
        onlyOpen: Boolean,
    ): List<SupportCase> =
        ModerationMappers.toDomainList(
            jpaRepository.search(type, status, priority, assigneeUserId, onlyOpen),
        )

    override fun findByOpenedBy(openedByUserId: UUID): List<SupportCase> =
        ModerationMappers.toDomainList(
            jpaRepository.findByOpenedByUserIdOrderByOpenedAtDesc(openedByUserId),
        )

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(case: SupportCase) {
        jpaRepository.save(ModerationMappers.toEntity(case))
    }
}

@Component
class CaseEventRepositoryAdapter(
    private val jpaRepository: CaseEventJpaRepository,
) : CaseEventRepository {

    override fun findByCase(caseId: UUID): List<CaseEvent> =
        ModerationMappers.toEventList(jpaRepository.findByCaseIdOrderByOccurredAtAsc(caseId))

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(event: CaseEvent) {
        jpaRepository.save(ModerationMappers.toEventEntity(event))
    }
}