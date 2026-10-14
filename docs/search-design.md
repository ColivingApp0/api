# Search — habitaciones con precio mensual

## Objetivo

Búsqueda de habitaciones publicadas con filtros combinables y orden determinista.
La unidad de resultado es la **habitación**, no la propiedad completa.

## Filtros reales

`q` (ciudad/barrio/propiedad/habitación), `cityId`, `minPrice`, `maxPrice`,
`currency`, `minCapacity`, `availableOnly` (por defecto `true`),
`airConditioning`, `fan`, `bathroomType`, `roomType`, `propertyId`, `services`,
`accessibilityCode`, `sort` (`RECENT`, `PRICE_ASC`, `PRICE_DESC`).

- Los precios son **mensuales**.
- `availableOnly` excluye habitaciones completas; con `false` se devuelven con
  etiqueta `Ocupada`.
- No hay fechas ni disponibilidad por rangos.

## Proyección pública

`PublishedListingInfo` → `ListingView`: propiedad, habitación, ciudad/barrio,
precio mensual/unidad, capacidad, ocupadas, libres, foto de habitación y de
propiedad, características y etiqueta de disponibilidad. **Nunca** incluye
identidad de residentes.

## Puertos

- `PublicationSearchQuery` (accommodation) → `PublicationCatalogPort` (search).
- La ocupación sale del mismo `UnitOccupancyPort` usado por accommodation.

## Endpoints

- `GET /api/v1/search/listings`
- Favoritos: `GET/POST/DELETE /api/v1/favorites[/{publicationId}]`.

## Tests

`SearchListingsServiceTest`: filtros por texto/ciudad, precio mensual, capacidad,
características, disponibilidad, orden determinista y validaciones.
