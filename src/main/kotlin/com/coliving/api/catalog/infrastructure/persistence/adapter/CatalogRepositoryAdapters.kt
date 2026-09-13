package com.coliving.api.catalog.infrastructure.persistence.adapter

import com.coliving.api.catalog.domain.enums.CatalogCategory
import com.coliving.api.catalog.domain.model.CatalogEntry
import com.coliving.api.catalog.domain.repository.CatalogRepository
import com.coliving.api.catalog.infrastructure.persistence.CatalogMappers
import com.coliving.api.catalog.infrastructure.persistence.repository.CatalogEntryJpaRepository
import java.util.UUID
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class CatalogRepositoryAdapter(
    private val jpaRepository: CatalogEntryJpaRepository,
) : CatalogRepository {

    override fun findById(id: UUID): CatalogEntry? =
        jpaRepository.findById(id).map(CatalogMappers::toDomain).orElse(null)

    override fun findByCategory(category: CatalogCategory, activeOnly: Boolean): List<CatalogEntry> =
        CatalogMappers.toDomainList(
            if (activeOnly) {
                jpaRepository.findByCategoryAndActiveOrderByNameAsc(category, true)
            } else {
                jpaRepository.findByCategoryOrderByNameAsc(category)
            },
        )

    override fun findByCategoryAndCode(category: CatalogCategory, code: String): CatalogEntry? =
        jpaRepository.findByCategoryAndCode(category, code)?.let(CatalogMappers::toDomain)

    override fun findByIds(ids: List<UUID>): List<CatalogEntry> =
        if (ids.isEmpty()) {
            emptyList()
        } else {
            CatalogMappers.toDomainList(jpaRepository.findByIdIn(ids))
        }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun save(entry: CatalogEntry) {
        jpaRepository.save(CatalogMappers.toEntity(entry))
    }
}