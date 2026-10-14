# Booking — solicitudes mensuales de plaza

> Modelo vigente desde `11.0.0-monthly-room-rental`: una solicitud pide una
> plaza de una habitación y no tiene fechas ni noches.

## Agregado `Reservation`

- Referencias por UUID a `accommodation_unit`, `accommodation_publication` e
  `identity_user`; FK solo a `identity_user`.
- `places` (plazas solicitadas; la UI solicita 1), mensaje, estado.
- Snapshot económico: `monthly_price`, `currency`, `price_unit` (RN-07).
- No existen `from_date`, `to_date`, `price_per_night`, holds ni expiraciones
  en la lógica nueva. Las columnas antiguas se conservan anulables solo para
  hidratar filas históricas.

## Estados

```
SOLICITADA ──> ACEPTADA     (ocupa la plaza de inmediato)
    ├────────> RECHAZADA    (terminal, no ocupa)
    └────────> CANCELADA    (terminal, libera si estaba aceptada)
INFORMACION_SOLICITADA es una decisión intermedia del anfitrión.
CONFIRMADA / EXPIRADA solo existen para datos históricos.
```

Cada transición registra `booking_reservation_event` (actor, tipo, motivo,
momento).

## Reglas

- Crear exige: habitación publicada, precio mensual configurado, plazas
  disponibles, huésped verificado (`COMPLETO`) y sin otra solicitud activa para
  la misma habitación.
- Aceptar bloquea la fila de la habitación (`SELECT … FOR UPDATE`), vuelve a
  comprobar plazas y luego cambia a `ACEPTADA`; dos aceptaciones concurrentes no
  pueden tomar la misma última plaza.
- Rechazar/cancelar una solicitud pendiente no consume plazas.
- Cancelar una aceptada libera la plaza automáticamente porque la ocupación se
  deriva de las solicitudes.
- Una restricción parcial única evita solicitudes activas duplicadas del mismo
  huésped en la misma habitación.

## Endpoints

Huésped (`/api/v1/reservations`, rol `HUESPED_*`):
`POST /` · `GET /mine` · `POST /{id}/cancel`.

Anfitrión (`/api/v1/host/reservations`, rol `ANFITRION`):
`GET /` · `GET /{id}/history` · `POST /{id}/accept|reject|request-info|cancel`.

## Tests

- `ReservationTest`: máquina de estados y snapshot mensual.
- `CreateReservationServiceTest`: gates de publicación, precio, plazas,
  duplicados y verificación.
- `MonthlyOccupancyIT`: ocupación real, pausa, consentimiento de compañeros y
  concurrencia de aceptación contra PostgreSQL (Testcontainers).
