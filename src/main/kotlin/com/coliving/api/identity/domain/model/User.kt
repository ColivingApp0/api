package com.coliving.api.identity.domain.model

import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.enums.UserStatus
import com.coliving.api.identity.domain.vo.Email
import com.coliving.api.identity.domain.vo.PasswordHash
import java.time.Instant
import java.util.UUID

/**
 * Aggregate root of the `identity` bounded context. Maps to the diagram's `Usuario`
 * entity, keeping its identity, authentication state (email + password per RF-001)
 * and role assignments. Authentication itself is performed by application services,
 * not by mutation methods on this aggregate.
 */
class User private constructor(
    val id: UUID,
    val email: Email,
    var passwordHash: PasswordHash,
    status: UserStatus,
    emailVerified: Boolean,
    val createdAt: Instant,
    roles: Set<UserRole>,
) {
    var status: UserStatus = status
        private set

    var emailVerified: Boolean = emailVerified
        private set

    private val rolesInternal: MutableSet<UserRole> = roles.toMutableSet()

    val roles: Set<UserRole>
        get() = rolesInternal.toSet()

    fun updatePassword(newHash: PasswordHash) {
        passwordHash = newHash
    }

    /**
     * RF-001: marks the e-mail as verified and promotes the account to ACTIVE.
     * Returns true if the state changed.
     */
    fun verifyEmail(): Boolean {
        if (emailVerified) return false
        emailVerified = true
        if (status == UserStatus.PENDING_EMAIL_VERIFICATION) {
            status = UserStatus.ACTIVE
        }
        return true
    }

    fun suspend() {
        if (status != UserStatus.SUSPENDED) status = UserStatus.SUSPENDED
    }

    fun activate() {
        if (status != UserStatus.ACTIVE) status = UserStatus.ACTIVE
    }

    fun isActive(): Boolean = status == UserStatus.ACTIVE

    /**
     * RF-004: assigns a role and enforces the invariant that a user has at most
     * one ACTIVE role (`rolActivo` in the diagram is a derived concept from
     * [UserRole.isActive], not a duplicated field).
     */
    fun assignRole(role: Role, assignedAt: Instant): UserRole {
        require(role.active) { "Cannot assign inactive role ${role.code}" }
        val existing = rolesInternal.find { it.roleId == role.id }
        if (existing != null) {
            if (!existing.isActive) existing.activate()
            return existing
        }
        rolesInternal.filter { it.isActive }.forEach { it.deactivate() }
        val userRole = UserRole.create(this.id, role, assignedAt)
        rolesInternal.add(userRole)
        return userRole
    }

    fun deactivateRole(roleId: UUID) {
        rolesInternal.firstOrNull { it.roleId == roleId }?.deactivate()
    }

    fun activeRoleCodes(): Set<RoleCode> =
        rolesInternal.filter { it.isActive }.map { it.roleCode }.toSet()

    fun hasActiveRole(code: RoleCode): Boolean = activeRoleCodes().contains(code)

    companion object {
        /**
         * Factory used by registration (RF-001). Status starts as PENDING_EMAIL_VERIFICATION.
         */
        fun register(email: Email, passwordHash: PasswordHash, now: Instant): User =
            User(
                id = UUID.randomUUID(),
                email = email,
                passwordHash = passwordHash,
                status = UserStatus.PENDING_EMAIL_VERIFICATION,
                emailVerified = false,
                createdAt = now,
                roles = emptySet(),
            )

        /**
         * Factory used by the persistence adapter to rebuild an aggregate from storage.
         */
        fun restore(
            id: UUID,
            email: Email,
            passwordHash: PasswordHash,
            status: UserStatus,
            emailVerified: Boolean,
            createdAt: Instant,
            roles: Set<UserRole>,
        ): User = User(id, email, passwordHash, status, emailVerified, createdAt, roles)
    }
}