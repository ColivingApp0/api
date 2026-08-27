package com.coliving.api.accommodation.domain.model

import com.coliving.api.accommodation.domain.enums.CancellationPolicy
import com.coliving.api.shared.error.InvalidArgumentException
import java.time.LocalTime
import java.util.UUID

/**
 * Booking rules of a publication: cancellation policy, check-in/out times,
 * min/max nights and house rules.
 */
class Rule(
    val id: UUID,
    val publicationId: UUID,
    cancellationPolicy: CancellationPolicy,
    checkInTime: LocalTime,
    checkOutTime: LocalTime,
    minNights: Int,
    maxNights: Int,
    houseRules: List<String>,
) {

    var cancellationPolicy: CancellationPolicy = cancellationPolicy
        private set

    var checkInTime: LocalTime = checkInTime
        private set

    var checkOutTime: LocalTime = checkOutTime
        private set

    var minNights: Int = minNights
        private set

    var maxNights: Int = maxNights
        private set

    var houseRules: List<String> = houseRules
        private set

    fun update(
        cancellationPolicy: CancellationPolicy,
        checkInTime: LocalTime,
        checkOutTime: LocalTime,
        minNights: Int,
        maxNights: Int,
        houseRules: List<String>,
    ) {
        if (minNights !in 1..maxNights) {
            throw InvalidArgumentException("minNights must be between 1 and maxNights")
        }
        this.cancellationPolicy = cancellationPolicy
        this.checkInTime = checkInTime
        this.checkOutTime = checkOutTime
        this.minNights = minNights
        this.maxNights = maxNights
        this.houseRules = houseRules.map { it.trim() }.filter { it.isNotBlank() }.distinct()
    }

    companion object {
        fun create(
            id: UUID,
            publicationId: UUID,
            cancellationPolicy: CancellationPolicy,
            checkInTime: LocalTime,
            checkOutTime: LocalTime,
            minNights: Int,
            maxNights: Int,
            houseRules: List<String>,
        ): Rule {
            val rule = Rule(
                id = id,
                publicationId = publicationId,
                cancellationPolicy = cancellationPolicy,
                checkInTime = checkInTime,
                checkOutTime = checkOutTime,
                minNights = minNights,
                maxNights = maxNights,
                houseRules = houseRules,
            )
            if (minNights !in 1..maxNights) {
                throw InvalidArgumentException("minNights must be between 1 and maxNights")
            }
            return rule
        }
    }
}