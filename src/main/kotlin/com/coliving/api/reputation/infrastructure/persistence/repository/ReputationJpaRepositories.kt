package com.coliving.api.reputation.infrastructure.persistence.repository

import com.coliving.api.reputation.infrastructure.persistence.entity.BenefitRuleEntity
import com.coliving.api.reputation.infrastructure.persistence.entity.EvaluationEntity
import com.coliving.api.reputation.infrastructure.persistence.entity.ReputationScoreEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface EvaluationJpaRepository : JpaRepository<EvaluationEntity, UUID> {

    fun findBySubjectUserIdOrderByCreatedAtDesc(subjectUserId: UUID): List<EvaluationEntity>

    fun findByAuthorUserIdOrderByCreatedAtDesc(authorUserId: UUID): List<EvaluationEntity>

    /** One evaluation per author and relation (RF-070). */
    fun existsByAuthorUserIdAndRelationId(authorUserId: UUID, relationId: UUID): Boolean
}

interface ReputationScoreJpaRepository : JpaRepository<ReputationScoreEntity, UUID>

interface BenefitRuleJpaRepository : JpaRepository<BenefitRuleEntity, UUID> {

    fun findByCode(code: String): BenefitRuleEntity?

    fun findByActiveTrueOrderByMinScoreAsc(): List<BenefitRuleEntity>

    fun findAllByOrderByMinScoreAsc(): List<BenefitRuleEntity>
}