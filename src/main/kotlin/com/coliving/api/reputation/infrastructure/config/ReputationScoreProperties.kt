package com.coliving.api.reputation.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

/**
 * Weights and version of the reputation formula, bound from `reputation.score`.
 * They are configuration (RF-071: "ponderaciones configurables") so the formula
 * can be tuned per environment while the version recorded with every score keeps
 * the results auditable.
 */
@ConfigurationProperties(prefix = "reputation.score")
data class ReputationScoreProperties(
    /** Version of the formula; bump it whenever the weights change. */
    val formulaVersion: Int = 1,
    val ratingWeight: BigDecimal = BigDecimal("0.5"),
    val staysWeight: BigDecimal = BigDecimal("0.2"),
    val verificationWeight: BigDecimal = BigDecimal("0.2"),
    val disputesWeight: BigDecimal = BigDecimal("0.1"),
)