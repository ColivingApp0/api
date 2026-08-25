package com.coliving.api.identity.infrastructure.persistence.entity

import com.coliving.api.identity.domain.enums.RoleCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(
    name = "identity_role",
    indexes = [Index(name = "uk_identity_role_code", columnList = "code", unique = true)],
)
class IdentityRoleEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: UUID = UUID.randomUUID(),

    @Enumerated(EnumType.STRING)
    @Column(name = "code", nullable = false, length = 50)
    var code: RoleCode = RoleCode.HUESPED_ESTUDIANTE,

    @Column(name = "name", nullable = false, length = 255)
    var name: String = "",

    @Column(name = "active", nullable = false)
    var active: Boolean = true,
)