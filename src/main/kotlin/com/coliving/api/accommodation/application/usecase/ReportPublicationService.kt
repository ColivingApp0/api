package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.port.out.PublicationReportCasePort
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.shared.error.InvalidArgumentException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Reports a publication (RF-025). Any authenticated user can report; the host
 * cannot report their own listing. The report itself opens a moderation case
 * (RF-082): the support queue is the single auditable record, and the reporter
 * can follow it up through their own case list.
 */
@Service
class ReportPublicationService(
    private val publicationRepository: PublicationRepository,
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val reportCasePort: PublicationReportCasePort,
) {

    @Transactional
    fun report(publicationId: UUID, reporterUserId: UUID, reason: String): UUID {
        val publication = publicationRepository.findById(publicationId)
            ?: throw NotFoundException("Publication not found")
        val unit = unitRepository.findById(publication.unitId)
            ?: throw NotFoundException("Unit not found")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Property not found")
        if (property.hostId == reporterUserId) {
            throw InvalidArgumentException("A host cannot report their own publication")
        }

        return reportCasePort.openPublicationReportCase(
            publicationId = publication.id,
            reportedUserId = property.hostId,
            reporterUserId = reporterUserId,
            reason = reason,
        )
    }
}