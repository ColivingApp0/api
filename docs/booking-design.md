# Contexto `booking` — Solicitudes de Reserva

Diseño del bounded context `booking` (SRS Sección 4, RF-040..RF-044; RN-03, RN-07).

## Objetivo

Gestionar el ciclo de vida de una solicitud de reserva (solicitud) que un
huésped presenta sobre una unidad publicada: creación, decisión del anfitrión,
confirmación, cancelación y expiración de la reserva provisional.

## Agregado

**`Reservation`** — referencias por UUID a `accommodation_unit`,
`accommodation_publication` e `identity_user` (sin FK cross-context; FK solo
hacia `identity_user`). Campos congelados al crear (RN-07, instantánea
económica): `pricePerNight`, `currency`, `cancellationPolicy`. El total se
deriva (`nights × pricePerNight`).

### Máquina de estados (RF-042)

```
SOLICITADA ──> INFORMACION_SOLICITADA ──┐
     │  ▲                               │ (host accepts)
     │  └───────────────────────────────┘
     ├──> ACEPTADA ──> CONFIRMADA        (guest confirms within hold window)
     ├──> RECHAZADA                      (host, terminal)
     ├──> CANCELADA                      (guest or host, terminal, reason required)
     └──> EXPIRADA                       (hold window elapsed, terminal)
```

Transiciones inválidas lanzan `ConflictException`; cada transición registra un
`ReservationEvent` (actor, tipo, motivo opcional, momento) — historial
consultable (RF-042).

## Reglas implementadas

| Req | Regla | Implementación |
|-----|-------|----------------|
| RF-040 | Solicitud con fechas, ocupantes y mensaje; solo si hay disponibilidad y el perfil cumple requisitos | `CreateReservationService`: `UnitCatalogPort` (PUBLICADA, RN-03) + capacidad + `GuestVerificationPort` (COMPLETO) + `UnitAvailabilityPort.isRangeAvailable` |
| RF-041 | Anfitrión acepta / rechaza / pide información | `AcceptReservationService`, `RejectReservationService`, `RequestInfoReservationService` con verificación de propiedad vía `UnitCatalogPort.isHostOfUnit` |
| RF-042 | Estados definidos + historial | Máquina de estados en el agregado + `booking_reservation_event` |
| RF-043 | Aceptación reserva provisional por periodo configurado; auto-liberación | `accept` ejecuta `lockRange` atómico y fija `holdExpiresAt` (`booking.hold-hours`, 24h); `ExpireStaleHoldsService` (@Scheduled 60s) expira y libera |
| RF-044 | Cancelación con motivo, por huésped o anfitrión | `CancelReservationService`: motivo obligatorio; si retiene inventario → `unlockRange` |
| RN-03 | Solo publicaciones activas reciben solicitudes | Gate `publicationStatus == PUBLICADA` en creación |
| RN-07 | Condiciones económicas congeladas | Snapshot en creación; los cambios posteriores de precio no afectan solicitudes existentes |

## Puertos (misma disciplina que identity/profile/accommodation)

Salida de booking (implementados por adapters en `booking/infrastructure/provider`):

- `UnitAvailabilityPort` → delega en accommodation's `ReservationAvailabilityPort`
  (`AvailabilityLockService`), único punto de aplicación del invariante de no
  doble-reserva. Se añadió `confirmRange` (BLOQUEADO → OCUPADO).
- `UnitCatalogPort` → accommodation `UnitBookingQuery` (nueva proyección de
  lectura: estado de publicación, precio, política, capacidad, unidades por host).
- `GuestVerificationPort` → identity `UserVerificationQuery` (COMPLETO),
  simétrico al gate de anfitrión de accommodation.

## Persistencia

`3.0.0-booking-schema.yaml`: `booking_reservation` (índices en guest, unit,
status+hold_expires_at) y `booking_reservation_event` (FK CASCADE a la
reserva, RESTRICT a `identity_user`).

## Endpoints

Guest (`/api/v1/reservations`, roles `HUESPED_*`):
`POST /` · `GET /mine` · `POST /{id}/confirm` · `POST /{id}/cancel`

Host (`/api/v1/host/reservations`, rol `ANFITRION`):
`GET /` · `GET /{id}/history` · `POST /{id}/accept` · `POST /{id}/reject` ·
`POST /{id}/request-info` · `POST /{id}/cancel`

## Tests

- `ReservationTest`: máquina de estados, snapshot RN-07, validaciones.
- `CreateReservationServiceTest`: gates RN-03/RF-040, snapshot.
- `ReservationLifecycleTest`: ciclo completo con fakes (accept→confirm,
  cancel→release, expiración RF-043, rechazo sin tocar inventario).
- `BookingAvailabilityIT`: flujo completo contra PostgreSQL real verificando
  estados de slots (BLOQUEADO→OCUPADO→DISPONIBLE) y no doble-reserva.
