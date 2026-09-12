# Community context

Bounded context that owns the **activities of a property and their
participants** (SRS group 04, RF-060..RF-062). It is the only context that
decides who belongs to the *authorized community* of a property and who may
take part in an activity.

## Requirements covered

| Req | Level | Implemented as |
| --- | --- | --- |
| RF-060 | Esencial | `CommunityActivity` created by the property host, enabled participants, cancellation |
| RF-061 | Esencial | `ActivityParticipant` confirms attendance under the capacity rule; withdrawal frees the place |
| RF-062 | Importante | `CommunitySummaryService`: aggregated counts, gated by the privacy consent |

## Model

```
CommunityActivity            ActivityParticipant
  id, propertyId, hostId       id, activityId, userId
  title, description           status: HABILITADO | CONFIRMADA | RETIRADA
  scheduledAt, capacity        registeredAt
  status: PROGRAMADA | CANCELADA
  createdAt
```

* The **enabled participants** are the authorized community of the activity:
  a row is written for each enabled resident when the activity is created, and
  only a user with such a row can read the activity or confirm attendance.
* `PROGRAMADA` vs `CANCELADA` is the only stored lifecycle state; "already
  happened" is derived from `scheduledAt`, so no scheduler is needed to keep it
  truthful.
* `capacity` bounds the number of `CONFIRMADA` rows at all times.

## Application services

| Service | Req | Notes |
| --- | --- | --- |
| `CreateActivityService` | RF-060 | Host-only; validates the date/capacity and that every enabled id is a **resident** of the property |
| `CancelActivityService` | RF-060 | Host-only; keeps the participant list for the record |
| `ListActivitiesService` | RF-060/061 | Host listing, resident listing and detail (authorized community only) |
| `ConfirmAttendanceService` | RF-061 | Locking read of the activity row, then the capacity check |
| `WithdrawFromActivityService` | RF-061 | Frees a place; the resident may confirm again |
| `CommunitySummaryService` | RF-062 | Counts only, `available` when at least one resident consented |

### Capacity invariant (RF-061)

`ActivityRepository.findByIdForUpdate` performs `SELECT ... FOR UPDATE` on the
activity row. Every confirmation reads that lock inside its transaction, counts
the confirmed participants and only then writes the new state, so two concurrent
confirmations cannot both take the last place. `CommunityCapacityIT` drives the
whole path against real PostgreSQL.

## Cross-context reads (no FK, no shared tables)

| Port (consumer side) | Provider query | Context |
| --- | --- | --- |
| `PropertyCommunityPort` | `PropertyCommunityQuery.hostOfProperty / propertyIdsOfHost / unitIdsOfProperty` | accommodation |
| `CommunityResidencyPort` | `ReservationCommunityQuery.confirmedGuestIds` | booking |
| `CommunityConsentPort` | `UserConsentQuery.hasAccepted(PRIVACIDAD)` | identity |

The residency of a property is **composed** in `BookingResidencyProvider`: the
unit ids come from accommodation and the confirmed guests from booking. The
community context never reads another context's tables; the user references are
plain UUIDs, while the FK rules of the database stay as defined by each
context (only users point to `identity_user`).

## Persistence

`6.0.0-community-schema.yaml` creates `community_activity` and
`community_activity_participant`. `property_id` is a plain UUID (no FK);
`host_id` and `user_id` carry FKs to `identity_user` (both `ON DELETE CASCADE`),
and the pair `(activity_id, user_id)` is unique.

## HTTP surface

| Method | Path | Who |
| --- | --- | --- |
| POST | `/api/v1/host/community/activities` | host (`ANFITRION`) |
| GET | `/api/v1/host/community/activities` | host |
| GET | `/api/v1/host/community/activities/{id}` | host |
| POST | `/api/v1/host/community/activities/{id}/cancel` | host |
| GET | `/api/v1/community/activities` | authenticated resident |
| GET | `/api/v1/community/activities/{id}` | authorized community |
| POST | `/api/v1/community/activities/{id}/confirm` | authorized community |
| POST | `/api/v1/community/activities/{id}/withdraw` | authorized community |
| GET | `/api/v1/community/summary?propertyId=` | authenticated |

Membership is not a role: the application service decides it from the
`community_activity_participant` rows.

## Out of scope (documented, not implemented)

* RF-062 currently aggregates **activities and resident counts**; other
  indicators (interests, ratings) belong to the reputation context.
* RF-052/RF-053 (blocking and reporting abusive users/conversations) depend on
  the moderation context (RF-081/RF-082).
