package com.coliving.api.reputation.infrastructure.config

import com.coliving.api.reputation.domain.model.ScoreFormula
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Wires the configurable formula of the reputation context: the properties
 * (`reputation.score.*`) are turned into the domain value object, so the rest of
 * the context depends on a validated formula instead of raw configuration.
 */
@Configuration
@EnableConfigurationProperties(ReputationScoreProperties::class)
class ReputationModuleConfig {

    @Bean
    fun scoreFormula(properties: ReputationScoreProperties): ScoreFormula =
        ScoreFormula(
            version = properties.formulaVersion,
            ratingWeight = properties.ratingWeight,
            staysWeight = properties.staysWeight,
            verificationWeight = properties.verificationWeight,
            disputesWeight = properties.disputesWeight,
        )
}