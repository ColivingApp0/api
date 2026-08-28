package com.coliving.api.identity.domain.enums

/**
 * Lifecycle of the account. Accounts are created in [PENDING_EMAIL_VERIFICATION]
 * and move to [ACTIVE] once the email is verified (RF-001). [SUSPENDED] blocks login.
 */
enum class UserStatus {
    PENDING_EMAIL_VERIFICATION,
    ACTIVE,
    SUSPENDED,
}

/**
 * Roles defined by the SRS v3.0 (section 1.3). VISITANTE is not a persisted role:
 * it represents the unauthenticated user, therefore it is excluded from this enum.
 */
enum class RoleCode {
    HUESPED_ESTUDIANTE,
    HUESPED_TURISTA,
    ANFITRION,
    RESIDENTE,
    MODERADOR,
    ADMINISTRADOR,
}

/**
 * Types of consent tracked per user (RF-005). Extensible without code changes
 * by adding values; the accept/revoke flow is generic.
 */
enum class ConsentType {
    TERMINOS,
    PRIVACIDAD,
    TRATAMIENTO_DATOS,
}

/**
 * Kind of verification document (RF-012). Extensible.
 */
enum class DocumentType {
    IDENTIDAD,
    CERTIFICADO_ACADEMICO,
    COMPROBANTE_ESTUDIO,
}

/**
 * Lifecycle of a verification document (diagram: enviar / revisar / rechazar / solicitar corrección).
 */
enum class DocumentStatus {
    PENDIENTE,
    APROBADO,
    RECHAZADO,
    CORRECCION_SOLICITADA,
}

/**
 * Verification level used as gate for sensitive operations (RF-014).
 * [NO_VERIFICADO] default; [BASICO] after completing profile data that counts as base verification;
 * [COMPLETO] after at least one document is approved.
 */
enum class VerificationLevel {
    NO_VERIFICADO,
    BASICO,
    COMPLETO,
}

/**
 * One-time security token purposes (email verification RF-001, password reset RF-003).
 */
enum class SecurityTokenPurpose {
    EMAIL_VERIFICATION,
    PASSWORD_RESET,
}