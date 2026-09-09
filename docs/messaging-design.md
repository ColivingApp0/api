# Contexto `messaging` — Mensajería y Notificaciones

Diseño del bounded context `messaging` (SRS grupo "Mensajería": RF-050 y RF-051;
RF-052 y RF-053 quedan parcialmente fuera de alcance).

## Objetivo

Permitir que el huésped y el anfitrión de una reserva conversen en un único
hilo asociado a esa solicitud, y notificar en la aplicación la llegada de cada
mensaje sin exponer contenido sensible.

## Alcance implementado

| Req | Regla | Implementación |
|-----|-------|----------------|
| RF-050 (Esencial) | Huésped y anfitrión conversan dentro de una solicitud | `Conversation` (una por reserva, creada de forma idempotente) + `ConversationService`; los mensajes se ordenan por `sentAt`, identifican al remitente y solo los dos participantes leen/escriben |
| RF-051 (Esencial) | Notificación de mensajes y cambios de estado; sin datos sensibles en pantalla bloqueada | `Notification` (payload sin contenido: etiqueta genérica + `referenceId`) y `NotificationService` (bandeja + contador de no leídas); se emite en la misma transacción del envío |

## Decisiones de diseño

- **Una conversación por reserva.** `messaging_conversation.reservation_id` es
  UNIQUE: ambas partes llegan siempre al mismo hilo, y `open` es idempotente
  (devuelve el existente si ya fue creado).
- **Autorización desde estado local.** Las dos partes se copian de `booking` al
  crear el hilo (`guest_user_id`, `host_user_id`), por lo que decidir quién
  puede leer o escribir no requiere llamar a otro contexto.
- **Notificación sin contenido.** `title` es la cadena fija
  `Tienes un nuevo mensaje` y `reference_id` apunta al hilo; ni el cuerpo del
  mensaje ni datos de la reserva se almacenan, de modo que una vista previa en
  pantalla bloqueada no filtra información (RF-051).
- **Límites de contenido.** `Message.send` normaliza (`trim`) y rechaza cuerpos
  vacíos o de más de 2000 caracteres: base de RF-053 sin inventar patrones de
  spam no definidos por el SRS.

## Pendiente (fuera de alcance actual)

- RF-052 (bloquear/reportar conversación): requiere el contexto de moderación
  (RF-081) para materializar casos; el bloqueo detendría nuevos mensajes.
- RF-053 (controles ante patrones definidos y eventos auditables): requiere la
  definición de patrones y el registro de auditoría (RF-085).
- RF-051 canales externos (correo/push): hoy solo canal in-app; la abstracción
  de `NotificationRepository` admite añadir despachadores sin cambiar el modelo.

## Puertos (disciplina cross-context existente)

- `ReservationPartiesPort` → booking `ReservationMessagingQuery`
  (nueva proyección de lectura `ReservationPartiesInfo`; mismo patrón que
  `UnitBookingQuery`).
- `UnitCatalogPort.hostOfUnit` (booking → accommodation `UnitBookingQuery`):
  ampliación mínima para resolver el anfitrión de una unidad, reutilizada por
  `hostOfUnit`/`isHostOfUnit` sin duplicar la lectura de propiedad.
- Referencias por UUID sin FK cross-context (`reservation_id`); `guest_user_id`,
  `host_user_id`, `sender_user_id`, `user_id` → `identity_user` (FK, regla del
  proyecto).

## Endpoints

- `GET /api/v1/conversations` — hilos del usuario autenticado (cualquiera de los
  dos lados).
- `POST /api/v1/conversations` — abre (o reabre) el hilo de una reserva.
- `GET /api/v1/conversations/{id}` — hilo con sus mensajes en orden y las
  fechas de la estancia en la cabecera.
- `POST /api/v1/conversations/{id}/messages` — envía un mensaje.
- `POST /api/v1/reservations/{reservationId}/messages` — envía abriendo el hilo
  si aún no existe (para clientes que solo conocen la reserva).
- `GET /api/v1/notifications` — bandeja con contador de no leídas.
- `POST /api/v1/notifications/{id}/read` — marca una notificación como leída.

## Tests

- `MessagingDomainTest`: dos partes distintas, participantes/contraparte,
  normalización y límite de cuerpo, notificación sin contenido y `markRead`
  idempotente.
- `ConversationServiceTest`: hilo único por reserva (idempotencia), lectura con
  orden cronológico, listado por ambas partes, rechazo a terceros y a reservas
  inexistentes.
- `MessageServiceTest`: persistencia con remitente, notificación a la
  contraparte (nunca al emisor), rechazos de terceros/contenido, y apertura del
  hilo a partir de la reserva.
- `NotificationServiceTest`: bandeja con contador, marcado de leído, propiedad y
  no encontrado.