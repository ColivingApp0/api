package com.coliving.api.shared.config

import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.PropertiesPropertySource

/**
 * Loads a local `.env` (or `.env.properties`) file from the working directory
 * into the Spring Environment before the application context is created.
 *
 * The file is optional and is meant for development: production should inject
 * real environment variables/secrets. Because the source is added last, OS
 * environment variables and command-line arguments still win.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class DotenvEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(
        environment: ConfigurableEnvironment,
        application: SpringApplication,
    ) {
        // Tests must be deterministic and must never load local secrets.
        if (System.getProperty("spring.dotenv.enabled") == "false") {
            return
        }
        val file = listOf(Path.of(".env"), Path.of(".env.properties"))
            .firstOrNull { Files.isRegularFile(it) }
            ?: return
        val properties = Properties()
        Files.newBufferedReader(file).use { properties.load(it) }
        environment.propertySources.addLast(
            PropertiesPropertySource(
                "dotenv:${file.toAbsolutePath()}",
                properties,
            ),
        )
    }
}
