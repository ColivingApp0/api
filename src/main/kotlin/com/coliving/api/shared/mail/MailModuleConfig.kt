package com.coliving.api.shared.mail

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/** Enables [MailProperties] for every module that sends e-mail. */
@Configuration
@EnableConfigurationProperties(MailProperties::class)
class MailModuleConfig
