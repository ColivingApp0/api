package com.coliving.api.identity.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import java.time.Clock

/**
 * Module wiring for `identity`. Enables the security tuning properties and
 * exposes a [Clock] so every service uses the same time source (testable).
 */
@Configuration
@EnableConfigurationProperties(IdentitySecurityProperties::class)
class IdentityModuleConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()
}