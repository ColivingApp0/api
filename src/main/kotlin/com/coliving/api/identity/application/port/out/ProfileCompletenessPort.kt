package com.coliving.api.identity.application.port.out

import com.coliving.api.identity.domain.enums.RoleCode
import java.util.UUID

/**
 * Out port declared by `identity` but implemented inside the `profile` bounded
 * context. Identity consumes it to enforce RF-004 (role changes require the
 * profile data relevant to the target role) without depending on `profile`.
 */
interface ProfileCompletenessPort {
    fun hasMinimumDataFor(userId: UUID, roleCode: RoleCode): Boolean
}