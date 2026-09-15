package com.coliving.api.reputation.infrastructure.provider

import com.coliving.api.booking.application.query.ReservationEvaluationQuery
import com.coliving.api.reputation.application.port.out.EligibleRelation
import com.coliving.api.reputation.application.port.out.EligibleRelationPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter over booking's [ReservationEvaluationQuery]: reputation resolves the
 * parties of an evaluation and the confirmed stays of a user without reading
 * reservation tables (RF-070, RF-071).
 */
@Component
class BookingEvaluationProvider(
    private val reservationEvaluationQuery: ReservationEvaluationQuery,
) : EligibleRelationPort {

    override fun findRelation(relationId: UUID): EligibleRelation? =
        reservationEvaluationQuery.relationOf(relationId)?.let { info ->
            EligibleRelation(
                relationId = info.reservationId,
                guestUserId = info.guestUserId,
                hostUserId = info.hostUserId,
                status = info.status,
                fromDate = info.fromDate,
                toDate = info.toDate,
            )
        }

    override fun confirmedStayCount(userId: UUID): Int =
        reservationEvaluationQuery.confirmedStayCount(userId)
}