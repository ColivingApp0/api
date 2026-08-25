package com.coliving.api.identity.domain.vo

/**
 * Value object for an e-mail address. Normalizes to lower-case on creation;
 * invites rejection of duplicates early (RF-001).
 */
class Email private constructor(val value: String) {

    init {
        require(REGEX.matches(value)) { "Invalid email address" }
    }

    override fun equals(other: Any?): Boolean =
        this === other || (other is Email && value == other.value)

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        private val REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

        fun of(raw: String): Email {
            val normalized = raw.trim().lowercase()
            return Email(normalized)
        }

        fun isValid(raw: String): Boolean = REGEX.matches(raw.trim().lowercase())
    }
}