# Contexto `search` — Búsqueda y Favoritos

Diseño del bounded context `search` (SRS grupo "Búsqueda": RF-030, RF-032,
RF-034; cierre del grupo 02 de casos de uso).

## Objetivo

Búsqueda pública de publicaciones con filtros combinables y ordenamiento
determinista, y gestión de favoritos de cada usuario sobre publicaciones
PUBLICADA.

## Alcance implementado

| Req | Regla | Implementación |
|-----|-------|----------------|
| RF-030 (Esencial) | Búsqueda por ciudad, fechas y presupuesto; resultados corresponden a criterios y reflejan disponibilidad conocida | `SearchListingsService`: filtros `cityId` (catálogo), rango de precio + moneda, disponibilidad del rango de fechas vía accommodation |
| RF-032 (Importante) | Orden por precio o actualización, determinista por consulta y versión de datos | `ListingSort.PRICE_ASC / PRICE_DESC / RECENT` con desempate por `publicationId` |
| RF-034 (Importante) | Favoritos por usuario sobre publicaciones | `FavoriteService` + `search_favorite` (UNIQUE user+publication), solo PUBLICADA |

## Pendiente (fuera de alcance actual)

- RF-030 parcial (sector, institución cercana): requiere catálogos de
  instituciones/sectores aún no modelados (RF-083 administración).
- RF-031 (filtros por tipo de habitación, servicios, accesibilidad): los
  amenities existen como texto libre en Property; falta normalización.
- RF-035 (afinidad): depende del contexto de reputación.

## Puertos (disciplina cross-context existente)

- `PublicationCatalogPort` → accommodation `PublicationSearchQuery`
  (nueva proyección de lectura: solo datos públicos, RN-02 — la ubicación
  exacta queda restringida, se expone `cityId`).
- `UnitAvailabilityPort` → accommodation `ReservationAvailabilityPort`
  (única fuente de verdad del estado de inventario).
- `Favorite` referencia la publicación por UUID (sin FK cross-context);
  `search_favorite.user_id` → `identity_user` (FK, regla del proyecto).

## Cambios mínimos en accommodation

- `PublicationSearchQuery` + `PublicationSearchQueryService` (proyección de
  lectura, mismo patrón que `UnitBookingQuery`).
- `Publication` ahora porta `createdAt/updatedAt` (con `touch()`) para
  soportar el orden por actualización; `PublicationService` lo actualiza en
  cada transición. Sin cambios de comportamiento en reglas existentes.

## Endpoints

- `GET /api/v1/search/listings` — filtros opcionales combinables
  (`cityId`, `minPrice`, `maxPrice`, `currency`, `availableFrom`,
  `availableTo`, `sort`). Usuario autenticado.
- `GET/POST/DELETE /api/v1/favorites` — favoritos del principal autenticado.

## Tests

- `SearchListingsServiceTest`: filtros RF-030, orden determinista RF-032
  (incluye empates), validaciones de rango.
- `FavoriteServiceTest`: alta, duplicado, baja, validación PUBLICADA,
  aislamiento por usuario.
