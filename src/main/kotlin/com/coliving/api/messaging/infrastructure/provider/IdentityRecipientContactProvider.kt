package com.coliving.api.messaging.infrastructure.provider

import com.coliving.api.identity.application.query.UserContactQuery
import com.coliving.api.messaging.application.port.out.RecipientContact
import com.coliving.api.messaging.application.port.out.RecipientContactPort
import java.util.UUID
import org.springframework.stereotype.Component

/**
 * Adapter from the messaging e-mail port to identity's contact query. Keeps the
 * bounded-context rule: messaging never reads the identity aggregate directly.
 */
@Component
class IdentityRecipientContactProvider(
    private val userContactQuery: UserContactQuery,
) : RecipientContactPort {

    override fun contactOf(userId: UUID): RecipientContact? =
        userContactQuery.contactOf(userId)?.let { contact ->
            RecipientContact(userId = contact.userId, email = contact.email)
        }
}
