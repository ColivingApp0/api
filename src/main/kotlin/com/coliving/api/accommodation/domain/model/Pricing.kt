package com.coliving.api.accommodation.domain.model

import com.coliving.api.accommodation.domain.enums.Currency
import com.coliving.api.shared.error.InvalidArgumentException
import java.math.BigDecimal
import java.util.UUID

/**
 * Per-night base pricing of a publication.
 */
class Pricing(
    val id: UUID,
    val publicationId: UUID,
    basePricePerNight: BigDecimal,
    currency: Currency,
) {

    var basePricePerNight: BigDecimal = basePricePerNight
        private set

    var currency: Currency = currency
        private set

    fun update(basePricePerNight: BigDecimal, currency: Currency) {
        if (basePricePerNight.signum() <= 0) {
            throw InvalidArgumentException("basePricePerNight must be positive")
        }
        this.basePricePerNight = basePricePerNight
        this.currency = currency
    }

    companion object {
        fun create(id: UUID, publicationId: UUID, basePricePerNight: BigDecimal, currency: Currency): Pricing {
            val pricing = Pricing(id, publicationId, basePricePerNight, currency)
            if (basePricePerNight.signum() <= 0) {
                throw InvalidArgumentException("basePricePerNight must be positive")
            }
            return pricing
        }
    }
}