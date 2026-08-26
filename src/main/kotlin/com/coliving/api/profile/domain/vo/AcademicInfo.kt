package com.coliving.api.profile.domain.vo

import java.util.Objects
import java.util.UUID

/**
 * RF-011 / RF-013: optional academic context of a guest. It references the
 * institution, faculty and career catalogs of the academic bounded context
 * through plain identifiers (no JPA relationship, no FK) and keeps its own
 * visibility flag; [unlisted] covers the "information not listed" case.
 */
class AcademicInfo private constructor(
    val institutionId: UUID?,
    val facultyId: UUID?,
    val careerId: UUID?,
    val unlisted: Boolean,
    val visible: Boolean,
) {

    fun withVisible(visible: Boolean): AcademicInfo =
        AcademicInfo(institutionId, facultyId, careerId, unlisted, visible)

    fun isEmpty(): Boolean =
        institutionId == null && facultyId == null && careerId == null

    override fun equals(other: Any?): Boolean =
        this === other || (
            other is AcademicInfo &&
                institutionId == other.institutionId &&
                facultyId == other.facultyId &&
                careerId == other.careerId &&
                unlisted == other.unlisted &&
                visible == other.visible
            )

    override fun hashCode(): Int =
        Objects.hash(institutionId, facultyId, careerId, unlisted, visible)

    override fun toString(): String =
        "AcademicInfo(institutionId=$institutionId, facultyId=$facultyId, careerId=$careerId, unlisted=$unlisted, visible=$visible)"

    companion object {
        fun of(
            institutionId: UUID?,
            facultyId: UUID?,
            careerId: UUID?,
            unlisted: Boolean,
            visible: Boolean,
        ): AcademicInfo = AcademicInfo(institutionId, facultyId, careerId, unlisted, visible)

        fun empty(): AcademicInfo =
            AcademicInfo(institutionId = null, facultyId = null, careerId = null, unlisted = false, visible = false)
    }
}