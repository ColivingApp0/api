package com.coliving.api.profile.domain.vo

/**
 * RF-010 (stay preferences): ordered, non-duplicated preference codes (e.g.
 * wifi, pet, quiet). Persisted as a convenience comma-separated string.
 */
class StayPreferences private constructor(private val codes: List<String>) {

    fun codes(): List<String> = codes

    fun isEmpty(): Boolean = codes.isEmpty()

    override fun equals(other: Any?): Boolean =
        this === other || (other is StayPreferences && codes == other.codes)

    override fun hashCode(): Int = codes.hashCode()

    override fun toString(): String = codes.toString()

    companion object {
        fun of(codes: List<String>): StayPreferences {
            val normalized = codes
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            return StayPreferences(normalized)
        }

        fun empty(): StayPreferences = StayPreferences(emptyList())
    }
}