package com.coliving.api.profile.infrastructure.persistence.repository

import com.coliving.api.profile.infrastructure.persistence.entity.ProfileEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileJpaRepository : JpaRepository<ProfileEntity, UUID> {

    fun findByUserId(userId: UUID): ProfileEntity?
}