# Accommodation bounded context — design

> Status: **approved for implementation (v1)**. The availability model below was
> deliberately designed with the future `booking` consumer in mind; booking is
> **not** implemented.

## Scope and boundary

Owns the hosting domain: properties, units, publications (listings), pricing,
rules and availability. Cross-context facts are reached through ports:

```
identity
   ▲
   │ UserVerificationQuery (implemented by HostVerificationProvider)
accommodation                 booking (future)
   │
   ├── availability lock/release   ◄── consumed via a port once booking exists
   └── (publishes details)         ◄── consumed by search/booking
```

## Aggregates and persistence (Liquibase 2.0.0)

| Aggregate | Table | Key facts |
| --- | --- | --- |
| `Property` | `accommodation_property` | `host_id` → FK `identity_user`; `city_id` reference (no FK, academic catalog); amenities comma-separated. |
| `Unit` | `accommodation_unit` | `property_id` → FK property; capacity units (`max_guests`, `bedrooms`, `beds`, `bathrooms`); the **reservation unit**. |
| `Publication` | `accommodation_publication` | 1:1 `unit_id` (UNIQUE, FK); `status`; independent aggregate so it can pause/archive without touching the unit. |
| `Pricing` | `accommodation_pricing` | 1:1 `publication_id` (UNIQUE, FK); `base_price_per_night NUMERIC(12,2)` + `currency`. |
| `Rule` | `accommodation_rule` | 1:1 `publication_id` (UNIQUE, FK); cancellation policy, check-in/out times, `min_nights`/`max_nights`, house rules. |
| `AvailabilitySlot` | `accommodation_availability_slot` | **`UNIQUE (unit_id, date)`**; `state`; `reservation_id UUID NULL` as an **external reference only** (no JPA entity, no FK — booking owns reservations). |

Per-night pricing and per-night availability are naturally aligned: a stay is a
set of day slots.

## The availability model (critical decision)

**Day slots**, not ranges, not hybrid:

```
availability_slot(unit_id, date, state, reservation_id)
  UNIQUE (unit_id, date)
  half-open stay interval: [check_in, check_out)   →  5 nights = from..from+5
```

Rules:

1. **Absence of a row means DISPONIBLE** (positive availability is implicit).
2. **Lazy materialization**: rows are created **only when there is a reason**
   — host block, reservation lock, or a rule-driven block. There is **no**
   pre-materialized horizon (no bulk 18-month generation).
3. `DISPONIBLE` state is never stored redundantly; `state` transitions only
   through domain behavior (no public setters).

## States and transitions (domain-enforced)

```
DISPONIBLE ──lock(reservationId)──► BLOQUEADO ──confirm()──► OCUPADO
     ▲                                │   │
     │◄──── release() ────────────────┘   └──── release() ──►
     │◄──── release() ─────────────────────────────┘
     │
     └◄── blockByHost() / release() for host blocks (reservation_id = NULL)
```

- `DISPONIBLE`: nobody occupies the day.
- `BLOQUEADO`: a reservation is being processed (non-null `reservation_id`) or
  the host blocked the day (`reservation_id = NULL`).
- `OCUPADO`: confirmed reservation.

Transitions are guarded: every day must be `DISPONIBLE` before a `BLOQUEADO` is
allowed; `CONFIRMED`/`OCUPADO` requires a preceding `BLOQUEADO`; cancellations
always return to `DISPONIBLE`.

## Concurrency and locking strategy (no double booking)

`UNIQUE (unit_id, date)` alone is **not** the guarantee. The guarantee is:

```
UNIQUE (unit_id, date)
   + single transaction
   + SELECT ... FOR UPDATE
   + domain state machine
```

`AvailabilityLockService.lockRange(unitId, from, to, reservationId)`, exactly
one transaction:

1. `INSERT ... ON CONFLICT (unit_id, date) DO NOTHING` for every date in the
   range (`generate_series`), so every day exists even if previously implicit.
2. `SELECT ... FOR UPDATE` over those rows **in ascending date order**
   (deterministic order → no deadlocks between concurrent locks).
3. Validate **all** days are `DISPONIBLE`; otherwise roll back with `Conflict`.
4. Transition all to `BLOQUEADO` with `reservation_id = reservationId`; commit.

Two concurrent transactions:

```
TX A                          TX B
────────────────────────────────────────────
FOR UPDATE day01..day04
      └─► BLOQUEADO + COMMIT
                              FOR UPDATE (blocks until A commits)
                              └─► reads BLOQUEADO ─► FAIL (Conflict)
```

`unlockRange`/release/cancel flows follow the same transactional pattern and
only touch rows belonging to the same `reservation_id` (or host blocks with
`reservation_id IS NULL`), so nobody can release another party's hold.

Verification: `AvailabilityConcurrencyIT` runs two real concurrent
`lockRange` calls against PostgreSQL via Testcontainers and asserts
exactly one succeeds and one gets `ConflictException`; the domain state
machine is exercised by `AvailabilitySlotTest` and the locking pattern by
`AvailabilityLockServiceTest` (in-memory).

Explicitly **not used in v1**: `daterange` + `EXCLUDE USING gist` and the hybrid
slots+ranges design. Two mechanisms of truth for availability would be more
harmful than helpful today; the hybrid can be revisited when booking exists.

## Future booking integration (contemplated, implemented port)

- `booking` (future) will declare its own out-port (or reuse
  `ReservationAvailabilityPort`) to lock/release ranges. The port is already
  defined in `accommodation` and implemented by `@Service AvailabilityLockService`
  (application layer), so `booking` depends only on the port interface.
  The `HostVerificationPort` → `identity` and `UserVerificationQuery` ←
  `identity` direction is the same pattern as elsewhere in the codebase.
- Flow it must support: check → **lock** → create booking request →
  accept/reject → **confirm / release**.
- `reservation_id` is already in the slot model as a plain UUID to make that
  integration additive.

## API outline (v1, authenticated)

| Method | Path | Role / owner | Purpose |
| --- | --- | --- | --- |
| `POST` | `/api/v1/host/properties` | host | Create property. |
| `PATCH` | `/api/v1/host/properties/{id}` | host (owner) | Update property. |
| `POST` | `/api/v1/host/properties/{id}/units` | host (owner) | Add unit. |
| `PATCH` | `/api/v1/host/units/{id}` | host (owner) | Update unit. |
| `POST` | `/api/v1/host/units/{id}/publication` | host (owner) | Create draft publication. |
| `POST` | `/api/v1/host/publications/{id}/publish` | host (owner) | Publish (requires host verification COMPLETO — `HostVerificationPort`). |
| `POST` | `/api/v1/host/publications/{id}/pause\|hide\|archive` | host (owner) | Change visibility. |
| `PUT` | `/api/v1/host/publications/{id}/pricing` | host (owner) | Base price + currency. |
| `PUT` | `/api/v1/host/publications/{id}/rules` | host (owner) | Rules/min-max nights. |
| `PUT/DELETE` | `/api/v1/host/units/{id}/availability/blocks` | host (owner) | Host block / release dates. |
| `GET` | `/api/v1/publications` | any user | List published publications. |
| `GET` | `/api/v1/publications/{id}` | any user | Publication detail. |
| `GET` | `/api/v1/publications/{id}/availability?from&to` | any user | Day-level availability. |

Cross-context gate: `PublicationService.publish` verifies the owner's identity
verification level through `HostVerificationPort`; the application decides,
`identity` owns the level.

## Non-goals (v1)

- No `booking`, no date-lock REST endpoint exposed to users (lock is an
  application service for the future booking consumer).
- No search engine / geo distance / full-text.
- No seasonal/override pricing (base nightly price only).
- No `daterange`/EXCLUDE constraint, no hybrid availability.
- No rating/review data (those live in another future context).