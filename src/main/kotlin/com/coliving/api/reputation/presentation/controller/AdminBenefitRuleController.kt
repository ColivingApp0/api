package com.coliving.api.reputation.presentation.controller

import com.coliving.api.reputation.application.dto.BenefitRuleView
import com.coliving.api.reputation.application.dto.CreateBenefitRuleCommand
import com.coliving.api.reputation.application.usecase.BenefitRuleService
import com.coliving.api.reputation.presentation.dto.CreateBenefitRuleRequest
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * Administration of the benefit rules (RF-073). The admin namespace already
 * requires MODERADOR or ADMINISTRADOR (SecurityConfig), which keeps the
 * configuration of benefits with the team that owns the platform rules.
 */
@Tag(
    name = "Admin Benefit rules",
    description = "Benefit rules configuration (RF-073); the admin namespace requires MODERADOR or ADMINISTRADOR.",
)
@RestController
@RequestMapping("/api/v1/admin/benefit-rules")
class AdminBenefitRuleController(
    private val benefitRuleService: BenefitRuleService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: CreateBenefitRuleRequest): BenefitRuleView =
        benefitRuleService.create(
            CreateBenefitRuleCommand(
                code = request.code,
                description = request.description,
                minScore = request.minScore,
            ),
        )

    @GetMapping
    fun list(@RequestParam(required = false, defaultValue = "false") activeOnly: Boolean): List<BenefitRuleView> =
        benefitRuleService.list(activeOnly)

    /** Deactivates a rule without deleting it, so history stays referenciable. */
    @PostMapping("/{id}/deactivate")
    fun deactivate(@PathVariable id: UUID): BenefitRuleView = benefitRuleService.deactivate(id)
}