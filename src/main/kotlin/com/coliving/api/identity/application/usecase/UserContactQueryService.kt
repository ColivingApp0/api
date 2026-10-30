package com.coliving.api.identity.application.usecase

import com.coliving.api.identity.application.query.UserContact
import com.coliving.api.identity.application.query.UserContactQuery
import com.coliving.api.identity.domain.repository.UserRepository
import java.util.UUID
import org.springframework.stereotype.Service

/** Read-side adapter that exposes the e-mail of a user to other contexts. */
@Service
class UserContactQueryService(
    private val userRepository: UserRepository,
) : UserContactQuery {

    override fun contactOf(userId: UUID): UserContact? =
        userRepository.findById(userId)?.let { user ->
            UserContact(userId = user.id, email = user.email.value)
        }
}
