package com.coliving.api.reputation.domain.model

import com.coliving.api.reputation.domain.enums.EvaluationCategory
import com.coliving.api.reputation.domain.enums.EvaluationStatus
import com.coliving.api.reputation.domain.enums.RelationType
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.ForbiddenException
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

/**
 * Evaluation between the two parties of an eligible relation (RF-070): the
 * author rates the subject under the defined categories after a confirmed stay.
 *
 * Invariants of the requirement live here: the parties must be different, every
 * defined category must be rated inside 1..5, and an evaluation can only be
 * disputed by the user it evaluates.
 */
class Evaluation(
    val id: UUID,
    val authorUserId: UUID,
    val subjectUserId: UUID,
    val relationType: RelationType,
    val relationId: UUID,
    ratings: Map<EvaluationCategory, Int>,
    val comment: String?,
    var status: EvaluationStatus,
    val createdAt: Instant,
    var disputeCaseId: UUID?,
) {

    val ratings: Map<EvaluationCategory, Int> = ratings.toMap()

    /** Overall rating: the average of the rated categories. */
    fun overallRating(): BigDecimal {
        val sum = ratings.values.sum()
        return BigDecimal(sum).divide(BigDecimal(ratings.size), 2, RoundingMode.HALF_UP)
    }

    /** Only an ACTIVE evaluation counts for the score (RF-072). */
    fun countsForScore(): Boolean = status == EvaluationStatus.ACTIVA

    /** Fails unless [userId] is the evaluated user (only they can dispute). */
    fun requireSubject(userId: UUID) {
        if (userId != subjectUserId) {
            throw ForbiddenException("Only the evaluated user can dispute this evaluation")
        }
    }

    /**
     * Marks the evaluation as disputed and links the moderation case that will
     * resolve it (RF-072). While disputed, the evaluation no longer counts.
     */
    fun dispute(caseId: UUID) {
        if (status == EvaluationStatus.DISPUTADA) {
            throw ConflictException("The evaluation is already disputed")
        }
        status = EvaluationStatus.DISPUTADA
        disputeCaseId = caseId
    }

    companion object {
        const val MAX_COMMENT_LENGTH = 1000

        fun create(
            id: UUID,
            authorUserId: UUID,
            subjectUserId: UUID,
            relationType: RelationType,
            relationId: UUID,
            ratings: Map<EvaluationCategory, Int>,
            comment: String?,
            now: Instant,
        ): Evaluation {
            if (authorUserId == subjectUserId) {
                throw InvalidArgumentException("A user cannot evaluate themselves")
            }
            val missing = EvaluationCategory.entries.filterNot { ratings.containsKey(it) }
            if (missing.isNotEmpty()) {
                throw InvalidArgumentException("Every category must be rated: missing $missing")
            }
            val outOfRange = ratings.filterValues { it < MIN_RATING || it > MAX_RATING }
            if (outOfRange.isNotEmpty()) {
                throw InvalidArgumentException(
                    "Ratings must be between $MIN_RATING and $MAX_RATING",
                )
            }
            val normalizedComment = comment?.trim()?.takeIf { it.isNotEmpty() }
            if (normalizedComment != null && normalizedComment.length > MAX_COMMENT_LENGTH) {
                throw InvalidArgumentException(
                    "The comment must not exceed $MAX_COMMENT_LENGTH characters",
                )
            }
            return Evaluation(
                id = id,
                authorUserId = authorUserId,
                subjectUserId = subjectUserId,
                relationType = relationType,
                relationId = relationId,
                ratings = ratings,
                comment = normalizedComment,
                status = EvaluationStatus.ACTIVA,
                createdAt = now,
                disputeCaseId = null,
            )
        }

        const val MIN_RATING = 1
        const val MAX_RATING = 5
    }
}