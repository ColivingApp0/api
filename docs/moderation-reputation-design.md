# Moderation & Reputation — diseño de contexto

Estados: implementado (RF-082 + RF-070..RF-073, con los bloqueos/reportes de mensajería RF-052/RF-053 y la proyección de evaluación en booking).

## Moderación (`moderation`)

Casos de soporte (RF-082): **un solo artefacto** para reportes (RF-052) y
disputas de evaluación (RF-072).

- Agregado `SupportCase`: `ABIERTO → EN_REVISION → RESUELTO | RECHAZADO`
  (terminales). Descripción obligatoria, notas obligatorias al cerrar, sin
  auto-reportes. `resolutionHours()` derivado (tiempos de atención medibles).
- `CaseEvent`: trazabilidad inmutable (actor, nota, instante) de cada acción.
- `CaseIntake` (puerto de entrada cross-context): la única puerta por la que
  `messaging` y `reputation` abren casos. Nadie más escribe casos.
- Endpoints: `/api/v1/admin/moderation/**` (roles MODERADOR/ADMINISTRADOR).
- `messaging` 5.0.1: `messaging_user_block` (RF-053) y `messaging_report`
  (RF-052) — el reporte abre el caso vía el intake y bloquea al reportado en
  el emisor.

## Reputación (`reputation`)

- `Evaluation` (RF-070): 5 categorías obligatorias (1..5), solo contraparte de
  una estancia CONFIRMADA ( booking, `ReservationEvaluationQuery`), una
  evaluación por autor y evento, sin auto-evaluación.
- `ScoreFormula` (RF-071): factores documentados — rating (media de activas,
  0..1), estancias (satura en 3), verificación (COMPLETO 1 / BÁSICO 0.5) y
  disputas (penalización 1/(1+n), nunca veto). Escala 0..5, pesos configurables
  en `reputation.score.*` que deben sumar 1; cada cálculo guarda versión y
  factores (`reputation_score`), auditable.
- Disputa (RF-072): abre caso en moderation (puerto `DisputeCasePort`),
  `DISPUTADA` suspende su efecto en el score hasta resolución.
- `BenefitRule` (RF-073): beneficios como **datos** (código + score mínimo,
  configurables por el admin, se desactivan sin borrarse), aplicados al score
  vigente.
- Endpoints: `/api/v1/reviews` (partes), `/api/v1/reputation/{userId}` con
  factores, `/api/v1/admin/reputation/benefit-rules`.

## Integración

`reputation` consume booking (relaciones/estancias), identity (nivel de
verificación) y moderation (intake de disputas); `moderation` consume los
reportes de messaging. Todas las referencias cross-context son UUID sin FK;
cada contexto expone sus proyecciones de lectura y nobody lee tablas ajenas.
