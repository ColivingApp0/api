package com.coliving.api.identity.domain.repository

import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.vo.Email
import java.util.UUID

/**
 * Out port for persisting the [User] aggregate.
 */
interface UserRepository {
    fun findById(id: UUID): User?
    fun findByEmail(email: Email): User?
    fun existsByEmail(email: Email): Boolean
    fun save(user: User)
}