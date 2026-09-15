package com.coliving.api.accommodation.domain.model

import com.coliving.api.accommodation.domain.enums.PublicationStatus
import com.coliving.api.accommodation.domain.enums.ReviewDecision
import java.time.Instant
import java.util.UUID

/**
 * Listing exposing a unit (1:1). An independent aggregate so visibility can be
 * managed without touching the unit. The publish gate (verified host) lives in
 * the application layer.
 */
class Publication(
    val id: UUID,
    val unitId: UUID,
    title: String,
    status: PublicationStatus,
    val createdAt: Instant,
    reviewRequestedAt: Instant? = null,
    reviewDecision: ReviewDecision? = null,
    reviewModeratorId: UUID? = null,
    reviewNote: String? = null,
    reviewSourceStatus: PublicationStatus? = null,
) {

    var title: String = title
        private set

    var status: PublicationStatus = status
        private set

    /** Last transition moment (used by the search recency ordering, RF-032). */
    var updatedAt: Instant = createdAt
        private set

    /** When the host (or moderation) sent the listing to review (RF-022/RF-081). */
    var reviewRequestedAt: Instant? = reviewRequestedAt
        private set

    /** Last moderation decision; null while the listing has never been reviewed. */
    var reviewDecision: ReviewDecision? = reviewDecision
        private set

    var reviewModeratorId: UUID? = reviewModeratorId
        private set

    /** Mandatory note for RECHAZADA / CORRECCION_SOLICITADA decisions. */
    var reviewNote: String? = reviewNote
        private set

    /** State the listing came from when the review started. */
    var reviewSourceStatus: PublicationStatus? = reviewSourceStatus
        private set

    fun touch(now: Instant) {
        updatedAt = now
    }

    fun publish() {
        require(
            status in setOf(PublicationStatus.BORRADOR, PublicationStatus.PAUSADA, PublicationStatus.OCULTA),
        ) { "Cannot publish a publication in state $status" }
        status = PublicationStatus.PUBLICADA
    }

    fun pause() {
        require(status == PublicationStatus.PUBLICADA) { "Cannot pause a publication in state $status" }
        status = PublicationStatus.PAUSADA
    }

    fun hide() {
        require(status == PublicationStatus.PUBLICADA || status == PublicationStatus.PAUSADA) {
            "Cannot hide a publication in state $status"
        }
        status = PublicationStatus.OCULTA
    }

    fun archive() {
        status = PublicationStatus.ARCHIVADA
    }

    /**
     * Sends the listing to moderation (RF-022 "enviar a revisión", RF-081).
     * Allowed from BORRADOR (pre-publication review) and from PUBLICADA or
     * PAUSADA (post-publication review, "según configuración"): the source
     * state is remembered so the decision can restore or hide it.
     */
    fun requestReview(now: Instant) {
        require(
            status in setOf(PublicationStatus.BORRADOR, PublicationStatus.PUBLICADA, PublicationStatus.PAUSADA),
        ) { "Cannot request review from state $status" }
        reviewSourceStatus = status
        reviewRequestedAt = now
        reviewDecision = null
        reviewModeratorId = null
        reviewNote = null
        status = PublicationStatus.EN_REVISION
        updatedAt = now
    }

    /**
     * Records the moderation decision (RF-081). APROBADA publishes the listing
     * (first publication or restoration); RECHAZADA returns a draft to
     * BORRADOR and hides a published listing; CORRECCION_SOLICITADA always
     * returns to BORRADOR. Rejection and correction require a note.
     */
    fun resolveReview(
        decision: ReviewDecision,
        moderatorId: UUID,
        note: String?,
        now: Instant,
    ) {
        require(status == PublicationStatus.EN_REVISION) { "The publication is not under review" }
        if (decision != ReviewDecision.APROBADA) {
            require(!note.isNullOrBlank()) { "A note is required for $decision" }
        }
        val source = reviewSourceStatus ?: PublicationStatus.BORRADOR
        status = when (decision) {
            ReviewDecision.APROBADA -> PublicationStatus.PUBLICADA
            ReviewDecision.RECHAZADA -> if (source == PublicationStatus.BORRADOR) {
                PublicationStatus.BORRADOR
            } else {
                PublicationStatus.OCULTA
            }
            ReviewDecision.CORRECCION_SOLICITADA -> PublicationStatus.BORRADOR
        }
        reviewDecision = decision
        reviewModeratorId = moderatorId
        reviewNote = note?.trim()?.takeIf { it.isNotEmpty() }
        updatedAt = now
    }

    companion object {
        fun create(id: UUID, unitId: UUID, title: String, now: Instant): Publication =
            Publication(
                id = id,
                unitId = unitId,
                title = title.trim(),
                status = PublicationStatus.BORRADOR,
                createdAt = now,
            )
    }
}