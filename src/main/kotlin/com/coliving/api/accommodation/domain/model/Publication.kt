package com.coliving.api.accommodation.domain.model

import com.coliving.api.accommodation.domain.enums.PublicationStatus
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
) {

    var title: String = title
        private set

    var status: PublicationStatus = status
        private set

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

    companion object {
        fun create(id: UUID, unitId: UUID, title: String): Publication =
            Publication(
                id = id,
                unitId = unitId,
                title = title.trim(),
                status = PublicationStatus.BORRADOR,
            )
    }
}