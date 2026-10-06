package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.ConfigureCatalogReferencesCommand
import com.coliving.api.accommodation.application.dto.ConfigurePricingCommand
import com.coliving.api.accommodation.application.dto.CreatePublicationCommand
import com.coliving.api.accommodation.application.dto.PricingView
import com.coliving.api.accommodation.application.dto.PublicationDetailView
import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.dto.RoommateView
import com.coliving.api.accommodation.application.port.out.HostVerificationPort
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.enums.ReviewDecision
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PublicationService(
    private val publicationRepository: PublicationRepository,
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val hostVerificationPort: HostVerificationPort,
    private val assembler: AccommodationViewAssembler,
) {

    @Transactional
    fun createPublication(command: CreatePublicationCommand): PublicationView {
        val unit = unitRepository.findById(command.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, command.hostId)
        if (publicationRepository.findByUnit(command.unitId) != null) {
            throw ConflictException("La habitación ya tiene una publicación")
        }
        val publication = Publication.create(
            id = UUID.randomUUID(),
            unitId = command.unitId,
            title = command.title,
            now = Instant.now(),
        )
        publication.configureCatalogReferences(command.services, command.applicableRuleCodes, Instant.now())
        publicationRepository.save(publication)
        return assembler.publicationView(publication)
    }

    /** Publish gate: verified host and an explicit monthly price. */
    @Transactional
    fun publish(publicationId: UUID, hostId: UUID): PublicationView {
        val publication = requireOwned(publicationId, hostId)
        val unit = unitRepository.findById(publication.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        if (unit.monthlyPrice == null) {
            throw ConflictException("Define el precio mensual antes de publicar la habitación")
        }
        if (!hostVerificationPort.isVerifiedHost(hostId)) {
            throw ConflictException("Verifica tu identidad para publicar habitaciones")
        }
        publication.publish()
        publication.touch(Instant.now())
        publicationRepository.save(publication)
        return assembler.publicationView(publication)
    }

    @Transactional
    fun requestReview(publicationId: UUID, hostId: UUID): PublicationView =
        transition(publicationId, hostId) { it.requestReview(Instant.now()) }

    fun findForReview(): List<PublicationView> =
        publicationRepository.findByStatus(PublicationStatus.EN_REVISION).map(assembler::publicationView)

    fun findByStatusForAdmin(status: PublicationStatus): List<PublicationView> =
        publicationRepository.findByStatus(status).map(assembler::publicationView)

    @Transactional
    fun resolveReview(
        publicationId: UUID,
        moderatorId: UUID,
        decision: ReviewDecision,
        note: String?,
    ): PublicationView {
        val publication = publicationRepository.findById(publicationId)
            ?: throw NotFoundException("Publicación no encontrada")
        publication.resolveReview(decision, moderatorId, note, Instant.now())
        publicationRepository.save(publication)
        return assembler.publicationView(publication)
    }

    /** RF-031: updates the searchable catalog references of the listing. */
    @Transactional
    fun configureCatalogReferences(command: ConfigureCatalogReferencesCommand): PublicationView {
        val publication = requireOwned(command.publicationId, command.hostId)
        publication.configureCatalogReferences(command.services, command.applicableRuleCodes, Instant.now())
        publicationRepository.save(publication)
        return assembler.publicationView(publication)
    }

    @Transactional
    fun pause(publicationId: UUID, hostId: UUID): PublicationView =
        transition(publicationId, hostId) { it.pause() }

    @Transactional
    fun hide(publicationId: UUID, hostId: UUID): PublicationView =
        transition(publicationId, hostId) { it.hide() }

    @Transactional
    fun archive(publicationId: UUID, hostId: UUID): PublicationView =
        transition(publicationId, hostId) { it.archive() }

    /** Monthly pricing lives on the room; the publishing switch stays separate. */
    @Transactional
    fun configurePricing(command: ConfigurePricingCommand): PricingView {
        val publication = requireOwned(command.publicationId, command.hostId)
        val unit = unitRepository.findById(publication.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        unit.changeMonthlyPrice(command.monthlyPrice, command.currency, command.priceUnit)
        unitRepository.save(unit)
        return PricingView(
            publicationId = publication.id,
            monthlyPrice = unit.monthlyPrice,
            currency = unit.currency,
            priceUnit = unit.priceUnit,
        )
    }

    fun findAllPublished(): List<PublicationView> =
        publicationRepository.findAllPublished().map(assembler::publicationView)

    @Transactional(readOnly = true)
    fun findPublishedDetail(publicationId: UUID): PublicationDetailView? {
        val publication = publicationRepository.findById(publicationId) ?: return null
        if (publication.status != PublicationStatus.PUBLICADA) return null
        return toDetail(publication)
    }

    /** Roommates of a published shared room, only with explicit consent. */
    @Transactional(readOnly = true)
    fun roommates(publicationId: UUID): List<RoommateView> {
        val publication = publicationRepository.findById(publicationId)
            ?: throw NotFoundException("Publicación no encontrada")
        if (publication.status != PublicationStatus.PUBLICADA) {
            throw NotFoundException("Publicación no encontrada")
        }
        val unit = unitRepository.findById(publication.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        return assembler.roommates(unit.id)
    }

    private fun toDetail(publication: Publication): PublicationDetailView {
        val unit = unitRepository.findById(publication.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        val propertyView = assembler.propertyView(property)
        return PublicationDetailView(
            publication = assembler.publicationView(publication),
            property = propertyView.copy(photos = emptyList()),
            unit = assembler.unitView(unit),
            host = assembler.hostView(property.hostId),
            occupancy = assembler.occupancy(unit, published = true),
            propertyPhotos = propertyView.photos,
        )
    }

    private fun transition(
        publicationId: UUID,
        hostId: UUID,
        action: (Publication) -> kotlin.Unit,
    ): PublicationView {
        val publication = requireOwned(publicationId, hostId)
        action(publication)
        publication.touch(Instant.now())
        publicationRepository.save(publication)
        return assembler.publicationView(publication)
    }

    private fun requireOwned(publicationId: UUID, hostId: UUID): Publication {
        val publication = publicationRepository.findById(publicationId)
            ?: throw NotFoundException("Publicación no encontrada")
        val unit = unitRepository.findById(publication.unitId)
            ?: throw NotFoundException("Habitación no encontrada")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Propiedad no encontrada")
        checkOwner(property, hostId)
        return publication
    }

    private fun checkOwner(property: Property, hostId: UUID) {
        if (property.hostId != hostId) throw ForbiddenException("No eres el anfitrión de esta propiedad")
    }
}
