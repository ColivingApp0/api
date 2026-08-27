package com.coliving.api.accommodation.application.usecase

import com.coliving.api.accommodation.application.dto.ConfigurePricingCommand
import com.coliving.api.accommodation.application.dto.ConfigureRulesCommand
import com.coliving.api.accommodation.application.dto.CreatePublicationCommand
import com.coliving.api.accommodation.application.dto.PricingView
import com.coliving.api.accommodation.application.dto.PropertyView
import com.coliving.api.accommodation.application.dto.PublicationDetailView
import com.coliving.api.accommodation.application.dto.PublicationView
import com.coliving.api.accommodation.application.dto.RulesView
import com.coliving.api.accommodation.application.dto.UnitView
import com.coliving.api.accommodation.application.port.out.HostVerificationPort
import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.model.Pricing
import com.coliving.api.accommodation.domain.model.Property
import com.coliving.api.accommodation.domain.model.Publication
import com.coliving.api.accommodation.domain.model.Rule
import com.coliving.api.accommodation.domain.model.Unit
import com.coliving.api.accommodation.domain.repository.PricingRepository
import com.coliving.api.accommodation.domain.repository.PropertyRepository
import com.coliving.api.accommodation.domain.repository.PublicationRepository
import com.coliving.api.accommodation.domain.repository.RuleRepository
import com.coliving.api.accommodation.domain.repository.UnitRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.NotFoundException
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class PublicationService(
    private val publicationRepository: PublicationRepository,
    private val unitRepository: UnitRepository,
    private val propertyRepository: PropertyRepository,
    private val pricingRepository: PricingRepository,
    private val ruleRepository: RuleRepository,
    private val hostVerificationPort: HostVerificationPort,
) {

    fun createPublication(command: CreatePublicationCommand): PublicationView {
        val unit = unitRepository.findById(command.unitId)
            ?: throw NotFoundException("Unit not found")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Property not found")
        checkOwner(property, command.hostId)
        val existing = publicationRepository.findByUnit(command.unitId)
        if (existing != null) throw ConflictException("Unit already has a publication")
        val publication = Publication.create(
            id = UUID.randomUUID(),
            unitId = command.unitId,
            title = command.title,
        )
        publicationRepository.save(publication)
        return publication.toView()
    }

    /** Publish gate (RF hosting): host must be identity-verified (COMPLETO). */
    fun publish(publicationId: UUID, hostId: UUID): PublicationView {
        val publication = requireOwned(publicationId, hostId)
        if (!hostVerificationPort.isVerifiedHost(hostId)) {
            throw ConflictException("Host identity is not verified; cannot publish")
        }
        publication.publish()
        publicationRepository.save(publication)
        return publication.toView()
    }

    fun pause(publicationId: UUID, hostId: UUID): PublicationView =
        transition(publicationId, hostId) { it.pause() }

    fun hide(publicationId: UUID, hostId: UUID): PublicationView =
        transition(publicationId, hostId) { it.hide() }

    fun archive(publicationId: UUID, hostId: UUID): PublicationView =
        transition(publicationId, hostId) { it.archive() }

    fun configurePricing(command: ConfigurePricingCommand): PricingView {
        requireOwned(command.publicationId, command.hostId)
        val pricing = pricingRepository.findByPublication(command.publicationId)
            ?: Pricing.create(
                id = UUID.randomUUID(),
                publicationId = command.publicationId,
                basePricePerNight = command.basePricePerNight,
                currency = command.currency,
            )
        pricing.update(command.basePricePerNight, command.currency)
        pricingRepository.save(pricing)
        return PricingView(
            publicationId = pricing.publicationId,
            basePricePerNight = pricing.basePricePerNight,
            currency = pricing.currency,
        )
    }

    fun configureRules(command: ConfigureRulesCommand): RulesView {
        requireOwned(command.publicationId, command.hostId)
        val rule = ruleRepository.findByPublication(command.publicationId)
            ?: Rule.create(
                id = UUID.randomUUID(),
                publicationId = command.publicationId,
                cancellationPolicy = command.cancellationPolicy,
                checkInTime = command.checkInTime,
                checkOutTime = command.checkOutTime,
                minNights = command.minNights,
                maxNights = command.maxNights,
                houseRules = command.houseRules,
            )
        rule.update(
            cancellationPolicy = command.cancellationPolicy,
            checkInTime = command.checkInTime,
            checkOutTime = command.checkOutTime,
            minNights = command.minNights,
            maxNights = command.maxNights,
            houseRules = command.houseRules,
        )
        ruleRepository.save(rule)
        return rule.toView()
    }

    fun findAllPublished(): List<PublicationView> =
        publicationRepository.findAllPublished().map { it.toView() }

    fun findPublishedDetail(publicationId: UUID): PublicationDetailView? {
        val publication = publicationRepository.findById(publicationId) ?: return null
        if (publication.status != PublicationStatus.PUBLICADA) return null
        return toDetail(publication)
    }

    private fun toDetail(publication: Publication): PublicationDetailView {
        val unit = unitRepository.findById(publication.unitId)!!
        val property = propertyRepository.findById(unit.propertyId)!!
        return PublicationDetailView(
            publication = publication.toView(),
            property = property.toView(),
            unit = unit.toView(),
            pricing = pricingRepository.findByPublication(publication.id)?.toView(),
            rules = ruleRepository.findByPublication(publication.id)?.toView(),
        )
    }

    private fun transition(
        publicationId: UUID,
        hostId: UUID,
        action: (Publication) -> kotlin.Unit,
    ): PublicationView {
        val publication = requireOwned(publicationId, hostId)
        action(publication)
        publicationRepository.save(publication)
        return publication.toView()
    }

    private fun requireOwned(publicationId: UUID, hostId: UUID): Publication {
        val publication = publicationRepository.findById(publicationId)
            ?: throw NotFoundException("Publication not found")
        val unit = unitRepository.findById(publication.unitId)
            ?: throw NotFoundException("Unit not found")
        val property = propertyRepository.findById(unit.propertyId)
            ?: throw NotFoundException("Property not found")
        checkOwner(property, hostId)
        return publication
    }

    private fun checkOwner(property: Property, hostId: UUID) {
        if (property.hostId != hostId) throw ForbiddenException("Not the owner of this property")
    }

    internal fun Publication.toView(): PublicationView =
        PublicationView(
            id = id,
            unitId = unitId,
            title = title,
            status = status,
        )

    internal fun Unit.toView(): UnitView =
        UnitView(id = id, propertyId = propertyId, name = name, maxGuests = maxGuests, bedrooms = bedrooms, beds = beds, bathrooms = bathrooms)

    internal fun Property.toView(): PropertyView =
        PropertyView(id = id, hostId = hostId, title = title, description = description, cityId = cityId, address = address, amenities = amenities, archived = archived)

    internal fun Pricing.toView(): PricingView =
        PricingView(publicationId = publicationId, basePricePerNight = basePricePerNight, currency = currency)

    internal fun Rule.toView(): RulesView =
        RulesView(
            publicationId = publicationId,
            cancellationPolicy = cancellationPolicy,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            minNights = minNights,
            maxNights = maxNights,
            houseRules = houseRules,
        )
}