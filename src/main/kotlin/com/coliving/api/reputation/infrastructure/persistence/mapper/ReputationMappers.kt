package com.coliving.api.reputation.infrastructure.persistence.mapper

import com.coliving.api.reputation.domain.enums.EvaluationCategory
import com.coliving.api.reputation.domain.model.BenefitRule
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.reputation.domain.model.ReputationScore
import com.coliving.api.reputation.domain.model.ScoreFactor
import com.coliving.api.reputation.infrastructure.persistence.entity.BenefitRuleEntity
import com.coliving.api.reputation.infrastructure.persistence.entity.EvaluationEntity
import com.coliving.api.reputation.infrastructure.persistence.entity.ReputationScoreEntity
import java.math.BigDecimal

/**
 * Pure domain <-> entity mapping for the reputation context.
 *
 * Two small codecs keep the schema flat: category ratings are stored as
 * `CATEGORY=value` pairs and the score factors as
 * `name|weight|normalizedValue|contribution` rows, both comma/newline separated.
 * They are internal to persistence, so the domain keeps real collections.
 */
object ReputationMappers {

    private const val RATING_SEPARATOR = ","
    private const val RATING_PAIR = "="
    private const val FACTOR_SEPARATOR = "\n"
    private const val FACTOR_FIELD = "|"

    fun toDomain(entity: EvaluationEntity): Evaluation =
        Evaluation(
            id = entity.id,
            authorUserId = entity.authorUserId,
            subjectUserId = entity.subjectUserId,
            relationType = entity.relationType,
            relationId = entity.relationId,
            ratings = decodeRatings(entity.ratings),
            comment = entity.comment,
            status = entity.status,
            createdAt = entity.createdAt,
            disputeCaseId = entity.disputeCaseId,
        )

    fun toEntity(evaluation: Evaluation): EvaluationEntity =
        EvaluationEntity(
            id = evaluation.id,
            authorUserId = evaluation.authorUserId,
            subjectUserId = evaluation.subjectUserId,
            relationType = evaluation.relationType,
            relationId = evaluation.relationId,
            ratings = encodeRatings(evaluation.ratings),
            overallRating = evaluation.overallRating(),
            comment = evaluation.comment,
            status = evaluation.status,
            disputeCaseId = evaluation.disputeCaseId,
            createdAt = evaluation.createdAt,
        )

    fun toDomainList(entities: List<EvaluationEntity>): List<Evaluation> = entities.map { toDomain(it) }

    fun toDomain(entity: ReputationScoreEntity): ReputationScore =
        ReputationScore(
            userId = entity.userId,
            score = entity.score,
            formulaVersion = entity.formulaVersion,
            factors = decodeFactors(entity.factors),
            computedAt = entity.computedAt,
        )

    fun toEntity(score: ReputationScore): ReputationScoreEntity =
        ReputationScoreEntity(
            userId = score.userId,
            score = score.score,
            formulaVersion = score.formulaVersion,
            factors = encodeFactors(score.factors),
            computedAt = score.computedAt,
        )

    fun toDomain(entity: BenefitRuleEntity): BenefitRule =
        BenefitRule(
            id = entity.id,
            code = entity.code,
            description = entity.description,
            minScore = entity.minScore,
            active = entity.active,
            createdAt = entity.createdAt,
        )

    fun toEntity(rule: BenefitRule): BenefitRuleEntity =
        BenefitRuleEntity(
            id = rule.id,
            code = rule.code,
            description = rule.description,
            minScore = rule.minScore,
            active = rule.active,
            createdAt = rule.createdAt,
        )

    private fun encodeRatings(ratings: Map<EvaluationCategory, Int>): String =
        ratings.entries.joinToString(RATING_SEPARATOR) { "${it.key}$RATING_PAIR${it.value}" }

    private fun decodeRatings(encoded: String): Map<EvaluationCategory, Int> =
        encoded.split(RATING_SEPARATOR)
            .filter { it.isNotBlank() }
            .associate { pair ->
                val (name, value) = pair.split(RATING_PAIR)
                EvaluationCategory.valueOf(name.trim()) to value.trim().toInt()
            }

    private fun encodeFactors(factors: List<ScoreFactor>): String =
        factors.joinToString(FACTOR_SEPARATOR) {
            listOf(it.name, it.weight.toPlainString(), it.normalizedValue.toPlainString(), it.contribution.toPlainString())
                .joinToString(FACTOR_FIELD)
        }

    private fun decodeFactors(encoded: String): List<ScoreFactor> =
        encoded.split(FACTOR_SEPARATOR)
            .filter { it.isNotBlank() }
            .map { row ->
                val fields = row.split(FACTOR_FIELD)
                ScoreFactor(
                    name = fields[0],
                    weight = BigDecimal(fields[1]),
                    normalizedValue = BigDecimal(fields[2]),
                    contribution = BigDecimal(fields[3]),
                )
            }

    /** Exposed for tests: the snapshots must round-trip the factor list. */
    fun roundTrip(score: ReputationScore): ReputationScore = toDomain(toEntity(score))
}