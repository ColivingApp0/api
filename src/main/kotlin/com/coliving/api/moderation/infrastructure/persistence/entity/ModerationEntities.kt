package com.coliving.api.moderation.infrastructure.persistence.entity

import com.coliving.api.moderation.domain.enums.CaseEventType
import com.coliving.api.moderation.domain.enums.CasePriority
import com.coliving.api.moderation.domain.enums.CaseStatus
import com.coliving.api.moderation.domain.enums.CaseSubjectType
import com.coliving.api.moderation.domain.enums.CaseType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "moderation_case")
class SupportCaseEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    var type: CaseType,

    @Enumerated(EnumType.STRING)
    @Column(name = "subject_type", nullable = false, length = 30)
    var subjectType: CaseSubjectType,

    // Cross-context reference to the reported object: plain UUID (no FK).
    @Column(name = "subject_id", nullable = false)
    var subjectId: UUID,

    @Column(name = "reported_user_id")
    var reportedUserId: UUID?,

    @Column(name = "opened_by_user_id")
    var openedByUserId: UUID?,

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    var priority: CasePriority,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: CaseStatus,

    @Column(name = "assignee_user_id")
    var assigneeUserId: UUID?,

    @Column(name = "resolution", columnDefinition = "TEXT")
    var resolution: String?,

    @Column(name = "opened_at", nullable = false)
    var openedAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Column(name = "closed_at")
    var closedAt: Instant?,
)

@Entity
@Table(name = "moderation_case_event")
class CaseEventEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "case_id", nullable = false)
    var caseId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    var eventType: CaseEventType,

    @Column(name = "actor_user_id", nullable = false)
    var actorUserId: UUID,

    @Column(name = "note", columnDefinition = "TEXT")
    var note: String?,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: Instant,
)