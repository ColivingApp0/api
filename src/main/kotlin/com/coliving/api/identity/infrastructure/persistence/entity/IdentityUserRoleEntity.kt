package com.coliving.api.identity.infrastructure.persistence.entity

import com.coliving.api.identity.domain.enums.RoleCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "identity_user_role",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_identity_user_role", columnNames = ["user_id", "role_id"]),
    ],
    indexes = [
        Index(name = "idx_identity_user_role_active", columnList = "user_id,is_active"),
    ],
)
class IdentityUserRoleEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "role_id", nullable = false)
    var roleId: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "role_code", nullable = false, length = 50)
    var roleCode: RoleCode = RoleCode.HUESPED_ESTUDIANTE,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "assigned_at", nullable = false)
    var assignedAt: Instant = Instant.now(),
)