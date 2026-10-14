# Accommodation — renta mensual de habitaciones

> Modelo vigente desde `11.0.0-monthly-room-rental`. Sustituye el diseño anterior
> de disponibilidad por días y precio por noche, que ya no se usa.

## Agregados

| Agregado | Tabla | Hechos clave |
| --- | --- | --- |
| `Property` | `accommodation_property` | `host_id` (FK `identity_user`), ciudad/barrio/dirección, amenities, fotos. |
| `Unit` (habitación) | `accommodation_unit` | Pertenencia a propiedad, capacidad (`max_guests`), precio mensual, unidad de cobro, tipo, baño, aire, ventilador, zonas comunes, fotos. |
| `Publication` | `accommodation_publication` | 1:1 con `unit`; estado de publicación (`BORRADOR`, `PUBLICADA`, `PAUSADA`, …). |
| `PropertyPhoto` / `UnitPhoto` | tablas propias | Listas ordenadas de URLs; la primera es la portada. |

## Capacidad, ocupación y disponibilidad

- La capacidad es `max_guests`.
- La ocupación **no se almacena**: se deriva de las solicitudes `ACEPTADA`
  (más las históricas `CONFIRMADA`) del contexto `booking`, a través de
  `UnitOccupancyPort`.
- Plazas libres = `max_guests - ocupadas` (nunca negativo).
- Disponibilidad efectiva = publicación `PUBLICADA` **y** plazas libres > 0.
- Pausar la publicación impide nuevas solicitudes aunque queden plazas.
- No hay calendarios, fechas, slots ni rangos de reserva.

## Precio

- `monthly_price NUMERIC(12,2)` + `currency` + `price_unit`
  (`POR_HABITACION`/`POR_PERSONA`).
- Las habitaciones individuales se cobran siempre por habitación.
- El precio se congela en la solicitud al crearse (RN-07).
- `NULL` significa que el anfitrión debe revisarlo antes de publicar; la
  migración pausó publicaciones históricas sin precio mensual.

## API

Host (`/api/v1/host`): propiedades, fotos de propiedad, habitaciones, fotos de
habitación, disponibilidad (`PUT /units/{id}/availability`), publicación y
precio mensual.

Público autenticado: `GET /publications`, `GET /publications/{id}`,
`GET /publications/{id}/roommates` (solo residentes con consentimiento) y
`GET /properties/{id}`.

## Fuera de alcance

- Subida de archivos de imagen (se gestionan URLs).
- Cálculo de disponibilidad por fechas.
- Precio por noche, reglas de estancia o bloqueos de calendario.
