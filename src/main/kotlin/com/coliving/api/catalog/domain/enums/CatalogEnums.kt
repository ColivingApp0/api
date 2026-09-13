package com.coliving.api.catalog.domain.enums

/**
 * Managed catalogs of the pilot (RF-083: "el administrador gestionará
 * catálogos de instituciones, facultades, carreras, ciudades, servicios y
 * reglas"). Room types and accessibility features are part of the same
 * managed catalog set because the listing filters (RF-031) consume them.
 *
 * Hierarchy rules: a FACULTAD belongs to an INSTITUCION and a CARRERA belongs
 * to a FACULTAD; the other categories are flat.
 */
enum class CatalogCategory {
    INSTITUCION,
    FACULTAD,
    CARRERA,
    CIUDAD,
    SERVICIO,
    REGLA,
    TIPO_HABITACION,
    ACCESIBILIDAD,
    ;

    /** Category a parent entry must belong to, or null when flat. */
    fun parentCategory(): CatalogCategory? = when (this) {
        FACULTAD -> INSTITUCION
        CARRERA -> FACULTAD
        else -> null
    }
}