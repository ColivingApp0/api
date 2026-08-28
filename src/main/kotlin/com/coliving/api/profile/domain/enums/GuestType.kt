package com.coliving.api.profile.domain.enums

/**
 * Kind of guest (RF-010). Mirrors "Huésped estudiante" / "Huésped turista".
 * Owned by the `profile` bounded context since it describes the profile, not
 * the identity.
 */
enum class GuestType {
    ESTUDIANTE,
    TURISTA,
}