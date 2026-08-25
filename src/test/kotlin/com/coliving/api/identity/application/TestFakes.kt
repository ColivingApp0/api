package com.coliving.api.identity.application

import com.coliving.api.identity.application.port.out.DocumentStorage
import com.coliving.api.identity.application.port.out.EmailSender
import com.coliving.api.identity.application.port.out.PasswordHasher
import com.coliving.api.identity.application.port.out.ProfileCompletenessPort
import com.coliving.api.identity.application.port.out.RawToken
import com.coliving.api.identity.application.port.out.TokenGenerator
import com.coliving.api.identity.domain.enums.ConsentType
import com.coliving.api.identity.domain.enums.DocumentStatus
import com.coliving.api.identity.domain.enums.RoleCode
import com.coliving.api.identity.domain.enums.SecurityTokenPurpose
import com.coliving.api.identity.domain.model.Consent
import com.coliving.api.identity.domain.model.Role
import com.coliving.api.identity.domain.model.SessionToken
import com.coliving.api.identity.domain.model.User
import com.coliving.api.identity.domain.model.VerificationDocument
import com.coliving.api.identity.domain.model.SecurityToken
import com.coliving.api.identity.domain.repository.ConsentRepository
import com.coliving.api.identity.domain.repository.RoleRepository
import com.coliving.api.identity.domain.repository.SecurityTokenStore
import com.coliving.api.identity.domain.repository.SessionTokenStore
import com.coliving.api.identity.domain.repository.UserRepository
import com.coliving.api.identity.domain.repository.VerificationDocumentRepository
import com.coliving.api.identity.domain.vo.Email
import java.time.Instant
import java.util.UUID

/** In-memory [UserRepository]. */
class FakeUserRepository : UserRepository {
    private val store = mutableMapOf<UUID, User>()

    override fun findById(id: UUID): User? = store[id]

    override fun findByEmail(email: Email): User? = store.values.firstOrNull { it.email == email }

    override fun existsByEmail(email: Email): Boolean = store.values.any { it.email == email }

    override fun save(user: User) {
        store[user.id] = user
    }
}

/** In-memory [RoleRepository] starting from the seeded SRS catalog. */
class FakeRoleRepository : RoleRepository {
    private val store = mutableMapOf<UUID, Role>()

    init {
        seed(RoleCode.HUESPED_ESTUDIANTE, "Huésped estudiante")
        seed(RoleCode.HUESPED_TURISTA, "Huésped turista")
        seed(RoleCode.ANFITRION, "Anfitrión")
        seed(RoleCode.RESIDENTE, "Residente")
        seed(RoleCode.MODERADOR, "Moderador / soporte")
        seed(RoleCode.ADMINISTRADOR, "Administrador")
    }

    private fun seed(code: RoleCode, name: String) {
        val id = UUID.nameUUIDFromBytes(code.name.toByteArray())
        store[id] = Role.of(id, code, name)
    }

    override fun findById(id: UUID): Role? = store[id]

    override fun findByCode(code: RoleCode): Role? = store.values.firstOrNull { it.code == code }

    override fun findAllActive(): List<Role> = store.values.filter { it.active }
}

/** In-memory [ProfileCompletenessPort]. */
class FakeProfileCompletenessPort : ProfileCompletenessPort {
    private val complete = mutableSetOf<Pair<UUID, RoleCode>>()

    override fun hasMinimumDataFor(userId: UUID, roleCode: RoleCode): Boolean =
        complete.contains(userId to roleCode)

    fun complete(userId: UUID, roleCode: RoleCode) {
        complete += userId to roleCode
    }
}

/** In-memory [ConsentRepository]. */
class FakeConsentRepository : ConsentRepository {
    private val store = mutableMapOf<Pair<UUID, ConsentType>, Consent>()

    override fun findByUserAndType(userId: UUID, type: ConsentType): Consent? =
        store[userId to type]

    override fun findByUser(userId: UUID): List<Consent> =
        store.filterKeys { it.first == userId }.values.toList()

    override fun save(consent: Consent) {
        store[consent.userId to consent.type] = consent
    }
}

/** In-memory [VerificationDocumentRepository]. */
class FakeVerificationDocumentRepository : VerificationDocumentRepository {
    private val store = mutableMapOf<UUID, VerificationDocument>()

    override fun findById(id: UUID): VerificationDocument? = store[id]

    override fun findByUser(userId: UUID): List<VerificationDocument> =
        store.values.filter { it.userId == userId }.sortedByDescending { it.uploadedAt }

    override fun findByStatus(status: DocumentStatus): List<VerificationDocument> =
        store.values.filter { it.status == status }.sortedByDescending { it.uploadedAt }

    override fun findAll(): List<VerificationDocument> =
        store.values.sortedByDescending { it.uploadedAt }

    override fun save(document: VerificationDocument) {
        store[document.id] = document
    }
}

/** In-memory [SessionTokenStore]. */
class FakeSessionTokenStore : SessionTokenStore {
    val store = mutableMapOf<String, SessionToken>()

    override fun save(token: SessionToken) {
        store[token.tokenHash] = token
    }

    override fun findByHash(hash: String): SessionToken? = store[hash]

    override fun revokeByHash(hash: String, now: Instant): Boolean {
        val token = store[hash] ?: return false
        token.revoke(now)
        return true
    }

    override fun revokeAllForUser(userId: UUID, now: Instant) {
        store.values.filter { it.userId == userId }.forEach { it.revoke(now) }
    }
}

/** In-memory [SecurityTokenStore]. */
class FakeSecurityTokenStore : SecurityTokenStore {
    val store = mutableMapOf<Pair<String, SecurityTokenPurpose>, SecurityToken>()

    override fun save(token: SecurityToken) {
        store[token.tokenHash to token.purpose] = token
    }

    override fun findByHashAndPurpose(hash: String, purpose: SecurityTokenPurpose): SecurityToken? =
        store[hash to purpose]

    override fun markUsed(id: UUID, now: Instant) {
        // The application consumed the in-memory domain object by reference;
        // the maps already hold the mutated state after consume().
    }
}

/** Fake [PasswordHasher]: "hash:<raw>" — fine for unit tests. */
class FakePasswordHasher : PasswordHasher {
    override fun hash(raw: String): String = "hash:$raw"

    override fun matches(raw: String, hash: String): Boolean = hash == "hash:$raw"
}

/** Fake [TokenGenerator] with deterministic hashing. */
class FakeTokenGenerator(private val prefix: String = "t") : TokenGenerator {
    private var counter = 0L

    override fun generate(): RawToken {
        counter += 1
        val raw = "$prefix-$counter"
        return RawToken(raw = raw, hash = hash(raw))
    }

    override fun hash(rawToken: String): String = "sha256($rawToken)"
}

/** Fake [EmailSender] capturing deliveries. */
class FakeEmailSender : EmailSender {
    val verificationEmails = mutableListOf<Pair<Email, String>>()
    val passwordResetEmails = mutableListOf<Pair<Email, String>>()

    override fun sendVerificationEmail(to: Email, rawToken: String) {
        verificationEmails += to to rawToken
    }

    override fun sendPasswordResetEmail(to: Email, rawToken: String) {
        passwordResetEmails += to to rawToken
    }
}

/** Fake [DocumentStorage]. */
class FakeDocumentStorage : DocumentStorage {
    val stored = mutableListOf<String>()
    val deleted = mutableListOf<String>()

    override fun store(userId: UUID, originalName: String, contentType: String, bytes: ByteArray): String {
        val key = "$userId/${originalName}"
        stored += key
        return key
    }

    override fun delete(storageKey: String) {
        deleted += storageKey
    }
}