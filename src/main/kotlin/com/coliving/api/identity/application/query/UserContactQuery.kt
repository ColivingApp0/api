package com.coliving.api.identity.application.query

import java.util.UUID

/**
 * Minimal contact projection owned by `identity`. Other bounded contexts (for
 * example `messaging`) use it to reach a user's e-mail without depending on the
 * identity aggregate.
 */
data class UserContact(
    val userId: UUID,
    val email: String,
)

interface UserContactQuery {
    fun contactOf(userId: UUID): UserContact?
}
