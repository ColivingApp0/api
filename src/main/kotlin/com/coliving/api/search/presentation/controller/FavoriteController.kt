package com.coliving.api.search.presentation.controller

import com.coliving.api.search.application.usecase.FavoriteService
import java.time.Instant
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import com.coliving.api.shared.security.CurrentUser
import io.swagger.v3.oas.annotations.tags.Tag

/**
 * Favorites of the authenticated user over published listings (RF-034).
 * Available to any authenticated role; ownership is implicit (the principal).
 */
@Tag(
    name = "Favorites",
    description = "Favorites of the authenticated user over published listings (RF-034).",
)
@RestController
@RequestMapping("/api/v1/favorites")
class FavoriteController(
    private val favoriteService: FavoriteService,
) {

    @GetMapping
    fun listMine(@AuthenticationPrincipal current: CurrentUser): List<FavoriteView> =
        favoriteService.listMine(current.userId).map { it.toView() }

    @PostMapping("/{publicationId}")
    @ResponseStatus(HttpStatus.CREATED)
    fun add(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable publicationId: UUID,
    ): FavoriteView = favoriteService.add(current.userId, publicationId).toView()

    @DeleteMapping("/{publicationId}")
    fun remove(
        @AuthenticationPrincipal current: CurrentUser,
        @PathVariable publicationId: UUID,
    ) = favoriteService.remove(current.userId, publicationId)

    private fun com.coliving.api.search.domain.model.Favorite.toView() =
        FavoriteView(id = id, publicationId = publicationId, createdAt = createdAt)
}

data class FavoriteView(
    val id: UUID,
    val publicationId: UUID,
    val createdAt: Instant,
)
