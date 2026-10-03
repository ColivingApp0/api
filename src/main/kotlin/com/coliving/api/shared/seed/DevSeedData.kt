package com.coliving.api.shared.seed

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Reproducible development data for the monthly room-rental flow.
 *
 * Enabled only with the `dev` profile (`app.seed.enabled`, on by default in
 * `application-dev.yml`). Every insert uses fixed UUIDs and `ON CONFLICT DO
 * NOTHING`, so running it repeatedly never duplicates data.
 *
 * Credentials are development-only and documented in the project report:
 * hosts use `Host123!`, guests use `Guest123!`.
 */
@Component
@Profile("dev")
class DevSeedData(
    private val jdbcTemplate: JdbcTemplate,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.seed.enabled:false}") private val enabled: Boolean,
) {

    private val log = LoggerFactory.getLogger(DevSeedData::class.java)

    private data class SeedUser(
        val id: String,
        val email: String,
        val name: String,
        val host: Boolean,
        val avatar: String,
        val roommateVisible: Boolean = false,
    )

    private data class SeedProperty(
        val id: String,
        val host: String,
        val title: String,
        val description: String,
        val city: String,
        val neighborhood: String,
        val address: String,
        val amenities: String,
        val photos: List<String>,
    )

    private data class SeedRoom(
        val id: String,
        val property: String,
        val name: String,
        val description: String,
        val capacity: Int,
        val price: String,
        val priceUnit: String,
        val roomType: String,
        val bathroom: String,
        val air: Boolean,
        val fan: Boolean,
        val commonAreas: String,
        val photos: List<String>,
        val published: Boolean = true,
        val sortOrder: Int = 0,
    )

    private data class SeedReservation(
        val id: String,
        val room: String,
        val guest: String,
        val status: String,
        val places: Int,
        val message: String?,
        val daysAgo: Int,
    )

    private val ids = { prefix: String, index: Int -> "$prefix%04d".format(index) }

    private val hosts = listOf(
        SeedUser("20000000-0000-0000-0000-000000000001", "ana@colivin.dev", "Ana María Rojas", true, "/images/avatars/avatar-01.svg"),
        SeedUser("20000000-0000-0000-0000-000000000002", "carlos@colivin.dev", "Carlos Mejía", true, "/images/avatars/avatar-02.svg"),
        SeedUser("20000000-0000-0000-0000-000000000003", "lucia@colivin.dev", "Lucía Moreno", true, "/images/avatars/avatar-03.svg"),
        SeedUser("20000000-0000-0000-0000-000000000004", "diego@colivin.dev", "Diego Ramírez", true, "/images/avatars/avatar-04.svg"),
    )

    private val guestNames = listOf(
        "Sofía Gómez", "Juan Pablo Ruiz", "Valentina García", "Mateo Castro",
        "Isabella Cruz", "Daniel Arias", "Laura Martínez", "Andrés Peña",
        "Camila Rincón", "Julián Sánchez", "Natalia Ríos", "Felipe Herrera",
        "Mariana Torres", "Santiago López", "Paula Andrea Díaz", "Emilio Gutiérrez",
    )

    private val guests: List<SeedUser> = guestNames.mapIndexed { index, name ->
        val n = index + 1
        SeedUser(
            id = "30000000-0000-0000-0000-0000000000%02d".format(n),
            email = "huesped%02d@colivin.dev".format(n),
            name = name,
            host = false,
            avatar = "/images/avatars/avatar-%02d.svg".format(n + 4),
            roommateVisible = n in setOf(1, 3, 5, 7, 9, 11, 14, 16),
        )
    }

    private val properties = listOf(
        SeedProperty("40000000-0000-0000-0000-000000000001", hosts[0].id, "Casa Chapinero Alto",
            "Casa restaurada con zonas comunes amplias, cocina equipada y terraza con vista a los cerros. A diez minutos del campus y del transporte público.",
            "Bogotá", "Chapinero Alto", "Calle 45 # 12-30",
            "Cocina equipada,Terraza,Zona de estudio,Lavandería,WiFi",
            listOf("/images/rooms/room-01.jpg", "/images/rooms/room-02.jpg", "/images/rooms/room-07.jpg")),
        SeedProperty("40000000-0000-0000-0000-000000000002", hosts[0].id, "Apartamento La Candelaria",
            "Apartamento luminoso en el centro histórico, ideal para estudiantes que quieren caminar a todo. Espacios comunes silenciosos y bien cuidados.",
            "Bogotá", "La Candelaria", "Carrera 3 # 11-24",
            "Cocina compartida,Sala,WiFi,Zona de estudio",
            listOf("/images/rooms/room-03.jpg", "/images/rooms/room-08.jpg")),
        SeedProperty("40000000-0000-0000-0000-000000000003", hosts[1].id, "Casa Laureles",
            "Casa familiar en Laureles con patio interior, cocina grande y un ambiente tranquilo. Cerca de universidades, supermercados y estación de metro.",
            "Medellín", "Laureles", "Circular 4 # 70-18",
            "Patio interior,Cocina equipada,Lavandería,WiFi,Parqueadero de bicicletas",
            listOf("/images/rooms/room-04.jpg", "/images/rooms/room-09.jpg", "/images/rooms/room-10.jpg")),
        SeedProperty("40000000-0000-0000-0000-000000000004", hosts[1].id, "Habitación El Poblado",
            "Habitación independiente en un edificio seguro de El Poblado, con excelente conexión al metro y a la zona universitaria.",
            "Medellín", "El Poblado", "Carrera 43A # 6 Sur-15",
            "WiFi,Baño privado,Zona de coworking",
            listOf("/images/rooms/room-05.jpg", "/images/rooms/room-11.jpg")),
        SeedProperty("40000000-0000-0000-0000-000000000005", hosts[2].id, "Casa Granada",
            "Casa amplia en Granada con jardín, cocina compartida y espacios pensados para estudiar y descansar. Ambiente diverso y respetuoso.",
            "Cali", "Granada", "Avenida 9N # 15-42",
            "Jardín,Cocina equipada,Sala,WiFi,Lavandería",
            listOf("/images/rooms/room-06.jpg", "/images/rooms/room-12.jpg")),
        SeedProperty("40000000-0000-0000-0000-000000000006", hosts[2].id, "Loft Norte",
            "Loft contemporáneo cerca del centro financiero, con cocina abierta y un espacio de trabajo compartido. Ideal para una estancia mensual tranquila.",
            "Bogotá", "Norte", "Calle 100 # 19-40",
            "Cocina abierta,Coworking,WiFi,Gimnasio",
            listOf("/images/rooms/room-07.jpg", "/images/rooms/room-02.jpg")),
        SeedProperty("40000000-0000-0000-0000-000000000007", hosts[3].id, "Casa Envigado",
            "Casa de dos pisos en un barrio residencial de Envigado, con patio, cocina compartida y habitaciones con baño privado.",
            "Medellín", "Envigado", "Calle 38 Sur # 43-12",
            "Patio,Cocina equipada,WiFi,Baño privado",
            listOf("/images/rooms/room-09.jpg", "/images/rooms/room-04.jpg")),
        SeedProperty("40000000-0000-0000-0000-000000000008", hosts[3].id, "Habitación Centro",
            "Habitación privada en el centro de Bogotá, a pocos minutos de universidades y bibliotecas. Servicios incluidos y excelente conectividad.",
            "Bogotá", "Centro", "Calle 19 # 5-40",
            "WiFi,Cocina compartida,Zona de estudio",
            listOf("/images/rooms/room-11.jpg", "/images/rooms/room-03.jpg")),
    )

    private val rooms = listOf(
        SeedRoom("50000000-0000-0000-0000-000000000001", properties[0].id, "Habitación individual con baño privado",
            "Habitación luminosa para una persona, con escritorio, armario y baño privado. Incluye servicios y acceso a todas las zonas comunes.",
            1, "1250000", "POR_HABITACION", "INDIVIDUAL", "PRIVADO", true, true,
            "Cocina,Terraza,Zona de estudio", listOf("/images/rooms/room-01.jpg", "/images/rooms/room-02.jpg"), sortOrder = 1),
        SeedRoom("50000000-0000-0000-0000-000000000002", properties[0].id, "Habitación compartida para dos",
            "Habitación amplia con dos camas, ventilador y baño compartido. Ideal para compartir con otra estudiante o profesional.",
            2, "780000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", false, true,
            "Cocina,Terraza,Lavandería", listOf("/images/rooms/room-03.jpg", "/images/rooms/room-07.jpg"), sortOrder = 2),
        SeedRoom("50000000-0000-0000-0000-000000000003", properties[0].id, "Habitación compartida para tres",
            "Habitación grande para tres personas, con aire acondicionado, camas individuales y baño compartido. Zonas comunes amplias.",
            3, "690000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", true, true,
            "Cocina,Terraza,Zona de estudio", listOf("/images/rooms/room-04.jpg", "/images/rooms/room-08.jpg"), sortOrder = 3),
        SeedRoom("50000000-0000-0000-0000-000000000004", properties[0].id, "Suite individual con entrada independiente",
            "Suite con entrada independiente, baño privado y aire acondicionado. La anfitriona la pausó temporalmente mientras renueva el mobiliario.",
            1, "1450000", "POR_HABITACION", "INDIVIDUAL", "PRIVADO", true, false,
            "Cocina,Terraza", listOf("/images/rooms/room-05.jpg"), published = false, sortOrder = 4),
        SeedRoom("50000000-0000-0000-0000-000000000005", properties[1].id, "Habitación compartida en La Candelaria",
            "Habitación para dos personas en apartamento histórico, con aire acondicionado y baño compartido. A cinco minutos de la Plaza de Bolívar.",
            2, "720000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", true, false,
            "Cocina,Sala,WiFi", listOf("/images/rooms/room-06.jpg", "/images/rooms/room-10.jpg"), sortOrder = 1),
        SeedRoom("50000000-0000-0000-0000-000000000006", properties[1].id, "Habitación individual La Candelaria",
            "Habitación individual con ventilador, escritorio y baño compartido. Un espacio sencillo y bien ubicado para estudiar.",
            1, "950000", "POR_HABITACION", "INDIVIDUAL", "COMPARTIDO", false, true,
            "Cocina,Sala,WiFi", listOf("/images/rooms/room-12.jpg"), sortOrder = 2),
        SeedRoom("50000000-0000-0000-0000-000000000007", properties[2].id, "Habitación individual en Laureles",
            "Habitación privada con baño interno y ventilador, en una casa familiar con patio. Ambiente tranquilo y cerca de todo.",
            1, "1100000", "POR_HABITACION", "INDIVIDUAL", "PRIVADO", false, true,
            "Patio,Cocina,WiFi", listOf("/images/rooms/room-02.jpg", "/images/rooms/room-09.jpg"), sortOrder = 1),
        SeedRoom("50000000-0000-0000-0000-000000000008", properties[2].id, "Habitación compartida con patio",
            "Habitación compartida para dos, con acceso directo al patio y baño compartido. Incluye servicios y lavandería.",
            2, "740000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", false, false,
            "Patio,Cocina,Lavandería", listOf("/images/rooms/room-03.jpg", "/images/rooms/room-11.jpg"), sortOrder = 2),
        SeedRoom("50000000-0000-0000-0000-000000000009", properties[2].id, "Habitación compartida para cuatro",
            "Habitación espaciosa para cuatro personas con aire acondicionado y baño compartido. Perfecta para un grupo de estudiantes.",
            4, "620000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", true, true,
            "Patio,Cocina,Lavandería,Zona de estudio", listOf("/images/rooms/room-04.jpg", "/images/rooms/room-12.jpg"), sortOrder = 3),
        SeedRoom("50000000-0000-0000-0000-000000000010", properties[3].id, "Habitación privada El Poblado",
            "Habitación individual con baño privado, aire acondicionado y escritorio. Edificio con seguridad y zonas comunes.",
            1, "1550000", "POR_HABITACION", "INDIVIDUAL", "PRIVADO", true, false,
            "WiFi,Coworking,Gimnasio", listOf("/images/rooms/room-05.jpg"), sortOrder = 1),
        SeedRoom("50000000-0000-0000-0000-000000000011", properties[4].id, "Habitación compartida Granada",
            "Habitación con dos camas y baño compartido, con acceso al jardín y a la cocina equipada. Excelente ambiente de casa.",
            2, "680000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", false, true,
            "Jardín,Cocina,Sala", listOf("/images/rooms/room-06.jpg", "/images/rooms/room-01.jpg"), sortOrder = 1),
        SeedRoom("50000000-0000-0000-0000-000000000012", properties[4].id, "Habitación individual Granada",
            "Habitación individual con baño interno y ventilador. Espacio tranquilo con mucha luz natural y armario amplio.",
            1, "980000", "POR_HABITACION", "INDIVIDUAL", "PRIVADO", false, true,
            "Jardín,Cocina,Sala", listOf("/images/rooms/room-07.jpg"), sortOrder = 2),
        SeedRoom("50000000-0000-0000-0000-000000000013", properties[4].id, "Habitación compartida para tres",
            "Habitación para tres personas con baño compartido y acceso a zonas comunes. La anfitriona la pausó para revisar disponibilidad.",
            3, "640000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", false, false,
            "Jardín,Cocina,Sala", listOf("/images/rooms/room-08.jpg"), published = false, sortOrder = 3),
        SeedRoom("50000000-0000-0000-0000-000000000014", properties[5].id, "Habitación compartida Loft Norte",
            "Habitación para dos personas con baño compartido y acceso al coworking del edificio. Ideal para una estancia mensual.",
            2, "860000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", true, true,
            "Coworking,Cocina,WiFi", listOf("/images/rooms/room-09.jpg", "/images/rooms/room-06.jpg"), sortOrder = 1),
        SeedRoom("50000000-0000-0000-0000-000000000015", properties[6].id, "Habitación individual Envigado",
            "Habitación privada con baño interno, aire acondicionado y acceso al patio. Casa residencial con ambiente familiar.",
            1, "1150000", "POR_HABITACION", "INDIVIDUAL", "PRIVADO", true, false,
            "Patio,Cocina,WiFi", listOf("/images/rooms/room-10.jpg"), sortOrder = 1),
        SeedRoom("50000000-0000-0000-0000-000000000016", properties[6].id, "Habitación compartida Envigado",
            "Habitación para dos personas con baño compartido, camas individuales y escritorio. Incluye servicios.",
            2, "700000", "POR_PERSONA", "COMPARTIDA", "COMPARTIDO", false, true,
            "Patio,Cocina,WiFi", listOf("/images/rooms/room-11.jpg"), sortOrder = 2),
        SeedRoom("50000000-0000-0000-0000-000000000017", properties[7].id, "Habitación individual Centro",
            "Habitación privada con baño compartido, ventilador y escritorio. A pocos minutos de universidades y transporte.",
            1, "890000", "POR_HABITACION", "INDIVIDUAL", "COMPARTIDO", false, true,
            "Cocina,WiFi,Zona de estudio", listOf("/images/rooms/room-12.jpg"), sortOrder = 1),
    )

    private val reservations = listOf(
        SeedReservation("60000000-0000-0000-0000-000000000001", rooms[0].id, guests[0].id, "ACEPTADA", 1, "Hola, me interesa la habitación desde el próximo mes.", 20),
        SeedReservation("60000000-0000-0000-0000-000000000002", rooms[1].id, guests[1].id, "ACEPTADA", 1, "Busco un espacio tranquilo para estudiar.", 18),
        SeedReservation("60000000-0000-0000-0000-000000000003", rooms[4].id, guests[2].id, "ACEPTADA", 1, "Me gustaría compartir con otra estudiante.", 16),
        SeedReservation("60000000-0000-0000-0000-000000000004", rooms[4].id, guests[3].id, "ACEPTADA", 1, "Estoy buscando una plaza en La Candelaria.", 15),
        SeedReservation("60000000-0000-0000-0000-000000000005", rooms[6].id, guests[4].id, "ACEPTADA", 1, "Soy estudiante de intercambio, llego el próximo mes.", 14),
        SeedReservation("60000000-0000-0000-0000-000000000006", rooms[8].id, guests[5].id, "ACEPTADA", 1, "Me interesa compartir la habitación grande.", 13),
        SeedReservation("60000000-0000-0000-0000-000000000007", rooms[8].id, guests[6].id, "ACEPTADA", 1, "Busco una plaza cerca de la universidad.", 12),
        SeedReservation("60000000-0000-0000-0000-000000000008", rooms[10].id, guests[7].id, "ACEPTADA", 1, "Me gustaría vivir en Granada.", 11),
        SeedReservation("60000000-0000-0000-0000-000000000009", rooms[15].id, guests[8].id, "ACEPTADA", 1, "Necesito una plaza en Envigado.", 10),
        SeedReservation("60000000-0000-0000-0000-000000000010", rooms[15].id, guests[9].id, "ACEPTADA", 1, "Comparto con una persona tranquila.", 9),
        // Pending requests: they do not occupy a place.
        SeedReservation("60000000-0000-0000-0000-000000000011", rooms[1].id, guests[10].id, "SOLICITADA", 1, "Hola, ¿sigue disponible la plaza?", 1),
        SeedReservation("60000000-0000-0000-0000-000000000012", rooms[7].id, guests[11].id, "SOLICITADA", 1, "Me interesa la habitación con patio.", 2),
        SeedReservation("60000000-0000-0000-0000-000000000013", rooms[10].id, guests[12].id, "INFORMACION_SOLICITADA", 1, "¿Incluye servicios?", 3),
        SeedReservation("60000000-0000-0000-0000-000000000014", rooms[13].id, guests[13].id, "SOLICITADA", 1, "Busco una habitación en el norte.", 1),
        SeedReservation("60000000-0000-0000-0000-000000000015", rooms[16].id, guests[14].id, "SOLICITADA", 1, "¿Hay disponibilidad para el próximo mes?", 4),
        SeedReservation("60000000-0000-0000-0000-000000000016", rooms[5].id, guests[15].id, "SOLICITADA", 1, "Me interesa la habitación individual.", 2),
        // Rejected and cancelled requests: no occupancy.
        SeedReservation("60000000-0000-0000-0000-000000000017", rooms[2].id, guests[0].id, "RECHAZADA", 1, "Solicitud rechazada por fechas de mudanza.", 8),
        SeedReservation("60000000-0000-0000-0000-000000000018", rooms[9].id, guests[1].id, "CANCELADA", 1, "La cancelé porque cambié de ciudad.", 6),
        SeedReservation("60000000-0000-0000-0000-000000000019", rooms[14].id, guests[2].id, "RECHAZADA", 1, "La habitación ya estaba reservada.", 7),
        SeedReservation("60000000-0000-0000-0000-000000000020", rooms[11].id, guests[3].id, "CANCELADA", 1, "Cancelada por cambio de planes.", 5),
    )

    @EventListener(ApplicationReadyEvent::class)
    fun seed() {
        if (!enabled) return
        val hostPassword = passwordEncoder.encode("Host123!") ?: error("BCrypt unavailable")
        val guestPassword = passwordEncoder.encode("Guest123!") ?: error("BCrypt unavailable")
        val now = Instant.now()

        hosts.forEach { insertUser(it, hostPassword, "ANFITRION", now) }
        guests.forEach { insertUser(it, guestPassword, "HUESPED_ESTUDIANTE", now) }

        properties.forEach { property ->
            jdbcTemplate.update(
                """
                INSERT INTO accommodation_property
                    (id, host_id, title, description, city_id, city_name, neighborhood, address,
                     amenities, archived, created_at, updated_at)
                VALUES (?::uuid, ?::uuid, ?, ?, NULL, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                property.id, property.host, property.title, property.description,
                property.city, property.neighborhood, property.address, property.amenities,
            )
            property.photos.forEachIndexed { index, url ->
                jdbcTemplate.update(
                    """
                    INSERT INTO accommodation_property_photo (id, property_id, url, position, created_at)
                    VALUES (?::uuid, ?::uuid, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (id) DO NOTHING
                    """.trimIndent(),
                    photoId(property.id, index), property.id, url, index,
                )
            }
        }

        rooms.forEach { room ->
            jdbcTemplate.update(
                """
                INSERT INTO accommodation_unit
                    (id, property_id, name, description, max_guests, monthly_price, currency,
                     price_unit, room_type, bathroom_type, air_conditioning, fan, common_areas,
                     bedrooms, beds, bathrooms, sort_order, created_at, updated_at)
                VALUES (?::uuid, ?::uuid, ?, ?, ?, ?::numeric, 'COP', ?, ?, ?, ?, ?, ?,
                        0, 1, 1, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                room.id, room.property, room.name, room.description, room.capacity,
                room.price, room.priceUnit, room.roomType, room.bathroom, room.air, room.fan,
                room.commonAreas, room.sortOrder,
            )
            room.photos.forEachIndexed { index, url ->
                jdbcTemplate.update(
                    """
                    INSERT INTO accommodation_unit_photo (id, unit_id, url, position, created_at)
                    VALUES (?::uuid, ?::uuid, ?, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (id) DO NOTHING
                    """.trimIndent(),
                    photoId(room.id, index), room.id, url, index,
                )
            }
            val status = if (room.published) "PUBLICADA" else "PAUSADA"
            jdbcTemplate.update(
                """
                INSERT INTO accommodation_publication
                    (id, unit_id, title, status, created_at, updated_at)
                VALUES (?::uuid, ?::uuid, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (unit_id) DO NOTHING
                """.trimIndent(),
                publicationId(room.id), room.id, room.name, status,
            )
        }

        reservations.forEach { reservation ->
            val room = rooms.first { it.id == reservation.room }
            val createdAt = now.minusSeconds(reservation.daysAgo.toLong() * 86_400)
            jdbcTemplate.update(
                """
                INSERT INTO booking_reservation
                    (id, unit_id, publication_id, guest_user_id, places, message, status,
                     monthly_price, currency, price_unit, created_at, updated_at)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, ?, ?, ?, ?::numeric, 'COP', ?, ?, ?)
                ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                reservation.id, reservation.room, publicationId(room.id), reservation.guest,
                reservation.places, reservation.message, reservation.status, room.price,
                room.priceUnit, java.sql.Timestamp.from(createdAt), java.sql.Timestamp.from(createdAt),
            )
            // History entry for the state the seeded request reached.
            jdbcTemplate.update(
                """
                INSERT INTO booking_reservation_event
                    (id, reservation_id, event_type, actor_user_id, reason, occurred_at)
                VALUES (?::uuid, ?::uuid, 'CREADA', ?::uuid, NULL, ?)
                ON CONFLICT (id) DO NOTHING
                """.trimIndent(),
                eventId(reservation.id, 1), reservation.id, reservation.guest,
                java.sql.Timestamp.from(createdAt),
            )
            if (reservation.status == "ACEPTADA") {
                jdbcTemplate.update(
                    """
                    INSERT INTO booking_reservation_event
                        (id, reservation_id, event_type, actor_user_id, reason, occurred_at)
                    VALUES (?::uuid, ?::uuid, 'ACEPTADA', ?::uuid, NULL, ?)
                    ON CONFLICT (id) DO NOTHING
                    """.trimIndent(),
                    eventId(reservation.id, 2), reservation.id, roomHost(room),
                    java.sql.Timestamp.from(createdAt.plusSeconds(3_600)),
                )
            }
        }

        log.info(
            "Development seed ready: {} hosts, {} guests, {} properties, {} rooms, {} requests",
            hosts.size, guests.size, properties.size, rooms.size, reservations.size,
        )
    }

    private fun roomHost(room: SeedRoom): String =
        properties.first { it.id == room.property }.host

    private fun insertUser(user: SeedUser, passwordHash: String, role: String, now: Instant) {
        jdbcTemplate.update(
            """
            INSERT INTO identity_user
                (id, email, password_hash, status, email_verified, created_at, updated_at)
            VALUES (?::uuid, ?, ?, 'ACTIVE', TRUE, ?, ?)
            ON CONFLICT DO NOTHING
            """.trimIndent(),
            user.id, user.email, passwordHash, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now),
        )
        val roleId = when (role) {
            "ANFITRION" -> "10000000-0000-0000-0000-000000000003"
            else -> "10000000-0000-0000-0000-000000000001"
        }
        jdbcTemplate.update(
            """
            INSERT INTO identity_user_role (id, user_id, role_id, role_code, is_active, assigned_at)
            VALUES (gen_random_uuid(), ?::uuid, ?::uuid, ?, TRUE, ?)
            ON CONFLICT (user_id, role_id) DO NOTHING
            """.trimIndent(),
            user.id, roleId, role, java.sql.Timestamp.from(now),
        )
        jdbcTemplate.update(
            """
            INSERT INTO profile_profile
                (user_id, full_name, guest_type, academic_unlisted, academic_visible,
                 affinity_visible, avatar_url, roommate_profile_visible, updated_at)
            VALUES (?::uuid, ?, ?, FALSE, FALSE, FALSE, ?, ?, ?)
            ON CONFLICT (user_id) DO NOTHING
            """.trimIndent(),
            user.id, user.name, if (user.host) null else "ESTUDIANTE",
            user.avatar, user.roommateVisible, java.sql.Timestamp.from(now),
        )
        jdbcTemplate.update(
            """
            INSERT INTO identity_verification_document
                (id, user_id, document_type, storage_key, status, uploaded_at, reviewed_at)
            VALUES (?::uuid, ?::uuid, 'IDENTIDAD', 'dev-seed/${user.id}', 'APROBADO', ?, ?)
            ON CONFLICT (id) DO NOTHING
            """.trimIndent(),
            UUID.nameUUIDFromBytes("document:${user.id}".toByteArray()).toString(),
            user.id, java.sql.Timestamp.from(now), java.sql.Timestamp.from(now),
        )
    }

    private fun photoId(ownerId: String, index: Int): String =
        UUID.nameUUIDFromBytes("photo:$ownerId:$index".toByteArray()).toString()

    private fun publicationId(unitId: String): String =
        UUID.nameUUIDFromBytes("publication:$unitId".toByteArray()).toString()

    private fun eventId(reservationId: String, index: Int): String =
        UUID.nameUUIDFromBytes("event:$reservationId:$index".toByteArray()).toString()
}
