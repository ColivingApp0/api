package com.coliving.api.reputation.application.port.out

import com.coliving.api.reputation.domain.enums.RelationType
import java.time.LocalDate
import java.util.UUID

/**
 * Relation snapshot consumed from `booking` (RF-070): the parties of a
 * reservation and whether it is an eligible stay to evaluate.
 */
data class EligibleRelation(
    val relationId: UUID,
    val guestUserId: UUID,
    val hostUserId: UUID,
    /** ReservationStatus name; only CONFIRMADA is eligible. */
    val status: String,
    val fromDate: LocalDate,
    val toDate: LocalDate,
) {
    fun parties(): Set<UUID> = setOf(guestUserId, hostUserId)

    fun counterpartOf(userId: UUID): UUID = when (userId) {
        guestUserId -> hostUserId
        hostUserId -> guestUserId
        else -> throw IllegalArgumentException("Not a party of this relation")
    }
}

/**
 * Relation and stay reads consumed from `booking`. Implemented by an adapter
 * delegating to booking's ReservationEvaluationQuery — reputation never reads
 * reservation tables.
 */
interface EligibleRelationPort {

    fun findRelation(relationId: UUID): EligibleRelation?

    /** Confirmed stays of a user, as guest or host (RF-071 factor). */
    fun confirmedStayCount(userId: UUID): Int
}

/**
 * Verification read consumed from `identity`: the verification factor of the
 * score (RF-071). Implemented by an adapter to identity's UserVerificationQuery.
 */
interface UserVerificationPort {

    /** VerificationLevel name of the user (NO_VERIFICADO / BASICO / COMPLETO). */
    fun levelOf(userId: UUID): String
}

/**
 * Case intake consumed from `moderation` (RF-072: "la disputa genera un caso").
 * Implemented by an adapter to moderation's CaseIntake; the returned id is
 * stored so the dispute can be followed up.
 */
interface DisputeCasePort {

    fun openEvaluationDisputeCase(
        evaluationId: UUID,
        evaluatedUserId: UUID,
        disputantUserId: UUID,
        reason: String,
    ): UUID
}