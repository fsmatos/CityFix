# 0002: Grouped reports share one ticket (no Group entity)

Status: accepted (Option A)

## Context
The brief (p.1, "Obs") says reports must be validated automatically: if a user reports a problem with the same type/situation within about 10 m of an existing one, a *group occurrence* is flagged in which several users follow the same problem in aggregate.

Two domain models were drawn:

- **Option A: one ticket, several citizens.** A new report with the same category + situation within 10 m does not create a ticket; the citizen starts following the existing ticket (`Municipe` *acompanha* `Ticket`, many-to-many). Diagram: [CityFix_Dominio_OpcaoA.png](../architecture/diagrams/CityFix_Dominio_OpcaoA.png).
- **Option B: one ticket per citizen plus a `Grupo`.** Every citizen keeps their own ticket; tickets with the same category + situation within 10 m are linked to a `Grupo` (2..* tickets). Diagram: [CityFix_Dominio_OpcaoB.png](../architecture/diagrams/CityFix_Dominio_OpcaoB.png).

The only structural difference is the `Grupo` entity (B) versus the `acompanha` association (A). Everything else is shared: `Utilizador` (specialised into `Municipe` and `Operador`), `Municipio`, `Categoria`, `Ticket`, `Mensagem`, `HistoricoEstado`, `Anexo` and the enums.

## Decision
Adopt **Option A**, because it is simpler: one lifecycle, one message thread and one history per problem, no group-versus-ticket state to keep consistent, and operators work on a single ticket.

## Consequences
- On submit, the app/repository looks for an existing active ticket with the same category + situation within 10 m. If found, the author is added to its followers instead of creating a ticket; otherwise a new ticket is created. This check must also work offline (against cached data) and be idempotent when sync happens later.
- `Municipe` creates 0..* tickets and follows 0..* tickets; a ticket has 1 creator and 0..* followers. State changes, messages and notifications (status change, info request, 20 m geofence, 24 h before closing) go to the creator and all followers.
- A follower's own attachments and description are not separate tickets; their extra media is attached to the shared ticket (`Anexo.carregado por`).
- Per-citizen reopening and private/public scope apply to the shared ticket; open question for planning: whether a follower can reopen it, and which author's `alcance` (public/private) wins.
- Anonymous users still never see author data or exact coordinates.
- If a future requirement needs per-citizen tickets, Option B can be adopted by adding `Grupo`; the diagrams are kept for the report.
