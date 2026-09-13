package com.coliving.api.accommodation.application.port.out

import java.util.UUID

/**
 * Case intake consumed from `moderation` (RF-025: reportar una publicación por
 * información engañosa, contenido inapropiado o posible fraude genera un caso
 * en la cola única de soporte). Implemented by an adapter delegating to
 * moderation's CaseIntake; the returned id is the case reference.
 */
interface PublicationReportCasePort {

    fun openPublicationReportCase(
        publicationId: UUID,
        reportedUserId: UUID,
        reporterUserId: UUID,
        reason: String,
    ): UUID
}