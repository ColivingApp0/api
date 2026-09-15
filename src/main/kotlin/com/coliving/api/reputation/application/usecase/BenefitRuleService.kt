package com.coliving.api.reputation.application.usecase

import com.coliving.api.reputation.application.dto.BenefitRuleView
import com.coliving.api.reputation.application.dto.CreateBenefitRuleCommand
import com.coliving.api.reputation.domain.model.BenefitRule
import com.coliving.api.reputation.domain.repository.BenefitRuleRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Administration of the benefit rules (RF-073): benefits are configured as data
 * (score threshold + code) instead of being hard-coded in the formula, which is
 * exactly what the requirement demands. Rules are deactivated, never deleted, so
 * historical references survive.
 */
@Service
class BenefitRuleService(
    private val benefitRuleRepository: BenefitRuleRepository,
) {

    @Transactional
    fun create(command: CreateBenefitRuleCommand): BenefitRuleView {
        val normalizedCode = command.code.trim().uppercase()
        if (benefitRuleRepository.findByCode(normalizedCode) != null) {
            throw ConflictException("A benefit rule with that code already exists")
        }
        val rule = BenefitRule.create(
            id = UUID.randomUUID(),
            code = normalizedCode,
            description = command.description,
            minScore = command.minScore,
            now = Instant.now(),
        )
        benefitRuleRepository.save(rule)
        return rule.toView()
    }

    @Transactional(readOnly = true)
    fun list(activeOnly: Boolean): List<BenefitRuleView> =
        benefitRuleRepository.findAll(activeOnly).map { it.toView() }

    @Transactional
    fun deactivate(ruleId: UUID): BenefitRuleView {
        val rule = benefitRuleRepository.findById(ruleId) ?: throw NotFoundException("Benefit rule not found")
        rule.deactivate()
        benefitRuleRepository.save(rule)
        return rule.toView()
    }
}

/** Shared projection of a benefit rule. */
internal fun BenefitRule.toView(): BenefitRuleView =
    BenefitRuleView(
        id = id,
        code = code,
        description = description,
        minScore = minScore,
        active = active,
        createdAt = createdAt,
    )