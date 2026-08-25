package com.coliving.api.identity.domain

import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.model.Consent
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConsentTest {

    private val now: Instant = Instant.parse("2026-09-01T10:00:00Z")
    private val userId = UUID.randomUUID()

    @Test
    fun `version and purpose are recorded on accept`() {
        val consent = Consent.create(userId, ConsentType.PRIVACIDAD, "v1", "tratamiento de datos", now)

        assertTrue(consent.accepted)
        assertEquals("v1", consent.version)
        assertEquals("tratamiento de datos", consent.purpose)
    }

    @Test
    fun `revoke keeps the latest version and purpose`() {
        val consent = Consent.create(userId, ConsentType.TERMINOS, "v1", "términos", now)

        consent.accept("v2", "términos actualizados", now.plusSeconds(60))
        consent.revoke(now.plusSeconds(120))

        assertFalse(consent.accepted)
        assertEquals("v2", consent.version)
        assertEquals("términos actualizados", consent.purpose)
    }
}