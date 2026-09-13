package com.coliving.api.moderation.infrastructure.persistence.mapper

import com.coliving.api.moderation.domain.model.CaseEvent
import com.coliving.api.moderation.domain.model.SupportCase
import com.coliving.api.moderation.infrastructure.persistence.entity.CaseEventEntity
import com.coliving.api.moderation.infrastructure.persistence.entity.SupportCaseEntity

/**
 * Pure domain <-> entity mapping, mirroring the other contexts: the aggregate is
 * rebuilt with its persisted state so storage never re-validates history.
 */
object ModerationMappers {

    fun toDomain(entity: SupportCaseEntity): SupportCase =
        SupportCase(
            id = entity.id,
            type = entity.type,
            subjectType = entity.subjectType,
            subjectId = entity.subjectId,
            reportedUserId = entity.reportedUserId,
            openedByUserId = entity.openedByUserId,
            description = entity.description,
            priority = entity.priority,
            status = entity.status,
            assigneeUserId = entity.assigneeUserId,
            resolution = entity.resolution,
            openedAt = entity.openedAt,
            updatedAt = entity.updatedAt,
            closedAt = entity.closedAt,
        )

    fun toEntity(case: SupportCase): SupportCaseEntity =
        SupportCaseEntity(
            id = case.id,
            type = case.type,
            subjectType = case.subjectType,
            subjectId = case.subjectId,
            reportedUserId = case.reportedUserId,
            openedByUserId = case.openedByUserId,
            description = case.description,
            priority = case.priority,
            status = case.status,
            assigneeUserId = case.assigneeUserId,
            resolution = case.resolution,
            openedAt = case.openedAt,
            updatedAt = case.updatedAt,
            closedAt = case.closedAt,
        )

    fun toDomainList(entities: List<SupportCaseEntity>): List<SupportCase> = entities.map { toDomain(it) }

    fun toEventDomain(entity: CaseEventEntity): CaseEvent =
        CaseEvent(
            id = entity.id,
            caseId = entity.caseId,
            type = entity.eventType,
            actorUserId = entity.actorUserId,
            note = entity.note,
            occurredAt = entity.occurredAt,
        )

    fun toEventEntity(event: CaseEvent): CaseEventEntity =
        CaseEventEntity(
            id = event.id,
            caseId = event.caseId,
            eventType = event.type,
            actorUserId = event.actorUserId,
            note = event.note,
            occurredAt = event.occurredAt,
        )

    fun toEventList(entities: List<CaseEventEntity>): List<CaseEvent> = entities.map { toEventDomain(it) }
}