package com.coliving.api.messaging.application.usecase

import com.coliving.api.messaging.application.dto.BlockView
import com.coliving.api.messaging.domain.model.UserBlock
import com.coliving.api.messaging.domain.repository.UserBlockRepository
import com.coliving.api.shared.error.ConflictException
import com.coliving.api.shared.error.NotFoundException
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Blocks between users (RF-053). A block is one way — the blocker decides —
 * but the message guard consults both directions, so no blocked user can write
 * into a shared conversation. Re-blocking an active block is a conflict.
 */
@Service
class BlockService(
    private val userBlockRepository: UserBlockRepository,
) {

    @Transactional
    fun block(blockerUserId: UUID, blockedUserId: UUID): BlockView {
        val existing = userBlockRepository.findByBlockerAndBlocked(blockerUserId, blockedUserId)
        if (existing != null) {
            throw ConflictException("The user is already blocked")
        }
        val block = UserBlock.create(blockerUserId, blockedUserId, Instant.now())
        userBlockRepository.save(block)
        return block.toView()
    }

    @Transactional
    fun unblock(blockerUserId: UUID, blockedUserId: UUID) {
        val block = userBlockRepository.findByBlockerAndBlocked(blockerUserId, blockedUserId)
            ?: throw NotFoundException("The user is not blocked")
        userBlockRepository.delete(block)
    }

    @Transactional(readOnly = true)
    fun listMine(blockerUserId: UUID): List<BlockView> =
        userBlockRepository.findByBlocker(blockerUserId).map { it.toView() }
}

/** Shared projection of a block. */
internal fun UserBlock.toView(): BlockView =
    BlockView(
        blockedUserId = blockedUserId,
        createdAt = createdAt,
    )