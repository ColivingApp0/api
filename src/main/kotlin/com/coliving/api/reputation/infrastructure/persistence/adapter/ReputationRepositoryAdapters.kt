package com.coliving.api.reputation.infrastructure.persistence.adapter

import com.coliving.api.reputation.domain.model.BenefitRule
import com.coliving.api.reputation.domain.model.Evaluation
import com.coliving.api.reputation.domain.model.ReputationScore
import com.coliving.api.reputation.domain.repository.BenefitRuleRepository
import com.coliving.api.reputation.domain.repository.EvaluationRepository
import com.coliving.api.reputation.domain.repository.ReputationScoreRepository
import com.coliving.api.reputation.infrastructure.persistence.mapper.ReputationMappers
import com.coliving.api.reputation.infrastructure.persistence.repository.BenefitRuleJpaRepository
import com.coliving.api.reputation.infrastructure.persistence.repository.EvaluationJpaRepository
import com.coliving.api.reputation.infrastructure.persistence.repository.ReputationScoreJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class EvaluationRepositoryAdapter(
    private val jpaRepository: EvaluationJpaRepository,
) : EvaluationRepository {

    override fun findById(id: UUID): Evaluation? =
        jpaRepository.findById(id).map(ReputationMappers::toDomain).orElse(null)

    override fun findBySubject(subjectUserId: UUID): List<Evaluation> =
        ReputationMappers.toDomainList(
            jpaRepository.findBySubjectUserIdOrderByCreatedAtDesc(subjectUserId),
        )

    override fun findByAuthor(authorUserId: UUID): List<Evaluation> =
        ReputationMappers.toDomainList(
            jpaRepository.findByAuthorUserIdOrderByCreatedAtDesc(authorUserId),
        )

    override fun existsByAuthorAndRelation(authorUserId: UUID, relationId: UUID): Boolean =
        jpaRepository.existsByAuthorUserIdAndRelationId(authorUserId, relationId)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(evaluation: Evaluation) {
        jpaRepository.save(ReputationMappers.toEntity(evaluation))
    }
}

@Component
class ReputationScoreRepositoryAdapter(
    private val jpaRepository: ReputationScoreJpaRepository,
) : ReputationScoreRepository {

    override fun findByUser(userId: UUID): ReputationScore? =
        jpaRepository.findById(userId).map(ReputationMappers::toDomain).orElse(null)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(score: ReputationScore) {
        jpaRepository.save(ReputationMappers.toEntity(score))
    }
}

@Component
class BenefitRuleRepositoryAdapter(
    private val jpaRepository: BenefitRuleJpaRepository,
) : BenefitRuleRepository {

    override fun findById(id: UUID): BenefitRule? =
        jpaRepository.findById(id).map(ReputationMappers::toDomain).orElse(null)

    override fun findByCode(code: String): BenefitRule? =
        jpaRepository.findByCode(code)?.let(ReputationMappers::toDomain)

    override fun findAll(activeOnly: Boolean): List<BenefitRule> =
        if (activeOnly) {
            jpaRepository.findByActiveTrueOrderByMinScoreAsc().map(ReputationMappers::toDomain)
        } else {
            jpaRepository.findAllByOrderByMinScoreAsc().map(ReputationMappers::toDomain)
        }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(rule: BenefitRule) {
        jpaRepository.save(ReputationMappers.toEntity(rule))
    }
}