package com.coliving.api.identity.domain.vo

/**
 * Value object representing an already-hashed password (RF-001 / RNF-001).
 * The raw password never lives in the domain model; only the hash does.
 */
class PasswordHash(val value: String) {

    init {
        require(value.isNotBlank()) { "Password hash must not be blank" }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is PasswordHash && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = "PasswordHash(**)"

    companion object {
        fun of(value: String): PasswordHash = PasswordHash(value)
    }
}