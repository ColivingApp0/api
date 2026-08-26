# Profile bounded context — implementation summary

Extracts `Profile` from the `identity` bounded context into a new `profile`
bounded context, following Clean Architecture and the SRS. `Consent` and
`VerificationDocument` stay in `identity`.

## Decisions

| Topic | Decision |
| --- | --- |
| Context split | `Profile` aggregate moves to `profile`. `Consent`/`VerificationDocument` remain in `identity` (SRS categorizes them as identity). |
| Cross-context communication | `ProfileCompletenessPort` (declared inside `identity`, implemented by a provider in `profile`) and `IdentityGateway` (declared inside `profile`, implementing the `UserVerificationQuery` contract of `identity`). No circular dependencies: `profile` only depends on `identity`'s query interface + enums; `identity` only depends on the port interface. |
| `verificationLevel` | Becomes a **derived** projection (`UserVerificationQueryService`): `COMPLETO` when at least one verification document is `APROBADO`, otherwise `NO_VERIFICADO`. It is never persisted in the profile. The old stored column is dropped. |
| Profile shape | Single `Profile` aggregate keyed by `user_id`. `GuestType` moved (its own enum in `profile`); `AcademicInfo` introduced as the academic Value Object (`institution_id`, `faculty_id`, `career_id` as plain identifiers referencing the academic catalog — no FK), plus `academic_visible`/`academic_unlisted`. `StayPreferences` moved as VO. No separate `GuestProfile`/`HostProfile` entities. |
| FK pattern | `profile_profile.user_id → identity_user.id` (same pattern as `identity_consent`). |
| Shared cross-cutting | `CurrentUser` moves to `shared.security`; `GlobalExceptionHandler`/`ErrorResponse` move to `shared.presentation.error` (global responsibility). |
| Lazy materialization | `profile_profile` rows are created on first PATCH/PUT (empty `Profile` → `ProfileView` is returned by GET for users without a profile). |

## Dependencies (no cycles)

```
identity ──? ProfileCompletenessPort (out-port, identity)
    ▲
profile ──? ProfileCompletenessProvider (profile)
profile ──? IdentityGateway (out-port, profile) ──? UserVerificationQuery (identity)
```

## Endpoints (authenticated unless noted)

| Method | Path | Context | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/profile` | profile | Current user's profile + derived verification level (RF-010). |
| `PATCH` | `/api/v1/profile` | profile | Partial profile update: personal data, guest type, city, academic refs, stay preferences (RF-010/RF-011). |
| `PUT` | `/api/v1/profile/privacy` | profile | Toggles `affinityVisible` and academic visibility (RF-013). |
| (unchanged) | `/api/v1/users/me/**` | identity | Account, consents, role selection, verification documents. |

## Tables

| Table | Owner | Notes |
| --- | --- | --- |
| `profile_profile` | profile | New (1.0.1): `user_id` PK → FK `identity_user`; personal data, guest type, city, academic refs, stay prefs, privacy flags. |
| `identity_profile` | — | Removed in 1.0.1 after data copy. |
| `identity_user`, `identity_role`, `identity_user_role`, `identity_consent`, `identity_verification_document`, `identity_*` | identity | Unchanged. |

### Liquibase 1.0.1 (`profile-context.yaml`)

1. Create `profile_profile` (+ `fk_profile_profile_user`).
2. Copy rows from `identity_profile` (`academic_*` fields default `false`/`NULL`, `verification_level` intentionally not copied).
3. Drop `fk_identity_profile_user` and `identity_profile`.

## Tests

- Unit: `profile/domain/ProfileTest`, `profile/application/*` (get/update/privacy), adjusted `SelectRoleServiceTest` (uses `FakeProfileCompletenessPort`).
- Web slice: `ProfileControllerTest` (`@WebMvcTest`, excludes `BearerTokenAuthenticationFilter`; CSRF included for state-changing calls).
- Integration (Testcontainers, ephemeral Postgres):
  - `ApiApplicationTests` – full context + production Liquibase master + JPA `validate`.
  - `ProfileMigrationIT` – dedicated DB; seeds populated `identity_profile`, applies the 1.0.1 changeset, asserts data copied to `profile_profile` and legacy table dropped.

> Note: `ApiApplicationTests` and `ProfileMigrationIT` use separate databases so changelog state never collides.

Run: `./gradlew test` (requires Docker; pulls `postgres:16-alpine`).

## Environment / CI note: Testcontainers version pin

`testcontainers-bom` is pinned to **1.21.4** on purpose. Testcontainers **1.21.3**
bundles a docker-java client that hardcodes Docker API **1.32** and fails in this
environment (Docker 29.x, minimum API 1.40) with:

```
BadRequestException (Status 400): client version 1.32 is too old.
Minimum supported API version is 1.40, please upgrade your client to a newer version
```

This surfaces as `"Could not find a valid Docker environment"` even though the
daemon is healthy (`docker info` works). `DOCKER_API_VERSION` does not help
because the old docker-java ignores it. Upgrading to 1.21.4 fixes negotiation
and tests run with no `DOCKER_HOST`/`DOCKER_API_VERSION` vars at all.

Quick diagnostic: run one IT with `--info` and check the
`DockerClientProviderStrategy` lines for the `client version ... too old` cause.

## Pending items / follow-ups

- Academic catalogs (academic context) do not exist yet: `institution_id`/`faculty_id`/`career_id` are reference-only columns with no FK.
- `VerificationLevel.BASICO` remains reserved (not currently reachable; derived projection only returns `NO_VERIFICADO`/`COMPLETO`).
- Future remote host: `GET /profile/{userId}` will need an explicit authorization policy (visibility rows already modeled).