package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.dto.ConversationReportView
import com.coliving.api.messaging.application.dto.ReportConversationCommand
import com.coliving.api.messaging.application.port.out.ReportCasePort
import com.coliving.api.messaging.domain.model.ConversationReport
import com.coliving.api.messaging.domain.repository.ConversationReportRepository
import com.coliving.api.messaging.domain.repository.ConversationRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reports of a conversation (RF-052). Only a party of the conversation can
 * report it, one open report per reporter and conversation, and every report
 * opens a support case in `moderation` — the shared auditable queue — whose id
 * is stored so the reporter can follow up.
 */
@Service
class ReportService(
    private val conversationRepository: ConversationRepository,
    private val reportRepository: ConversationReportRepository,
    private val reportCasePort: ReportCasePort,
) {

    @Transactional
    fun report(command: ReportConversationCommand): ConversationReportView {
        val conversation = conversationRepository.findById(command.conversationId)
            ?: throw NotFoundException("Conversation not found")
        conversation.requireParticipant(command.reporterUserId)
        val reportedUserId = conversation.counterpartOf(command.reporterUserId)

        val existing = reportRepository.findByConversationAndReporter(conversation.id, command.reporterUserId)
        if (existing != null) {
            throw ConflictException("This conversation was already reported")
        }

        val now = Instant.now()
        val caseId = reportCasePort.openConversationReportCase(
            conversationId = conversation.id,
            reportedUserId = reportedUserId,
            reporterUserId = command.reporterUserId,
            reason = command.reason,
        )
        val report = ConversationReport.create(
            id = UUID.randomUUID(),
            conversationId = conversation.id,
            reporterUserId = command.reporterUserId,
            reportedUserId = reportedUserId,
            reason = command.reason,
            caseId = caseId,
            now = now,
        )
        reportRepository.save(report)
        return report.toView()
    }

    @Transactional(readOnly = true)
    fun listMine(reporterUserId: UUID): List<ConversationReportView> =
        reportRepository.findByReporter(reporterUserId).map { it.toView() }
}

/** Shared projection of a conversation report. */
internal fun ConversationReport.toView(): ConversationReportView =
    ConversationReportView(
        id = id,
        conversationId = conversationId,
        reportedUserId = reportedUserId,
        reason = reason,
        caseId = caseId,
        createdAt = createdAt,
    )