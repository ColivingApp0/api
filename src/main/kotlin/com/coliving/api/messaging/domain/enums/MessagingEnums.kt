package com.coliving.api.messaging.domain.enums

/**
 * Kind of in-app notification (RF-051). Only the message event is defined
 * today: the notification center is deliberately open for further event types
 * (e.g. reservation state changes) without changing its storage model.
 */
enum class NotificationType {
    /** The counterpart wrote inside a reservation conversation (RF-050). */
    MENSAJE_NUEVO,
}