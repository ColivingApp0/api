package com.coliving.api.reputation.infrastructure.persistence.entity

import com.coliving.api.reputation.domain.enums.EvaluationStatus
import com.coliving.api.reputation.domain.enums.RelationType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reputation_evaluation")
class EvaluationEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "author_user_id", nullable = false)
    var authorUserId: UUID,

    @Column(name = "subject_user_id", nullable = false)
    var subjectUserId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    var relationType: RelationType,

    // Cross-context reference to the evaluated event (reservation): plain UUID (no FK).
    @Column(name = "relation_id", nullable = false)
    var relationId: UUID,

    /** Category ratings encoded as `CATEGORY=value` joined by commas. */
    @Column(name = "ratings", nullable = false, columnDefinition = "TEXT")
    var ratings: String,

    @Column(name = "overall_rating", nullable = false, precision = 3, scale = 2)
    var overallRating: BigDecimal,

    @Column(name = "comment", columnDefinition = "TEXT")
    var comment: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: EvaluationStatus,

    @Column(name = "dispute_case_id")
    var disputeCaseId: UUID?,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)

@Entity
@Table(name = "reputation_score_snapshot")
class ReputationScoreEntity(
    @Id @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "score", nullable = false, precision = 4, scale = 2)
    var score: BigDecimal,

    @Column(name = "formula_version", nullable = false)
    var formulaVersion: Int,

    /** Factors encoded as `name|weight|value|contribution` joined by newlines. */
    @Column(name = "factors", nullable = false, columnDefinition = "TEXT")
    var factors: String,

    @Column(name = "computed_at", nullable = false)
    var computedAt: Instant,
)

@Entity
@Table(name = "reputation_benefit_rule")
class BenefitRuleEntity(
    @Id @Column(name = "id", nullable = false)
    var id: UUID,

    @Column(name = "code", nullable = false, length = 60, unique = true)
    var code: String,

    @Column(name = "description", length = 200)
    var description: String?,

    @Column(name = "min_score", nullable = false, precision = 4, scale = 2)
    var minScore: BigDecimal,

    @Column(name = "active", nullable = false)
    var active: Boolean,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,
)