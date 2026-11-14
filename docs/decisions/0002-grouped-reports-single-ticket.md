# 0002: Grouped reports share one ticket (no Group entity)

Status: accepted (Option A)

Amended by F-012 (2026-11-14, Q5, see [decision 0014](0014-location-gate-grouping-map.md)):
- **The joiner's media stay in their own report.** A citizen who joins a ticket by grouping keeps their description **and** photos/videos in `tickets/{id}/reports/{uid}` (decision 0010), not on the shared ticket. The shared ticket only gains the follower. This replaces the consequence "their extra media is attached to the shared ticket" below. Reason: grouping ignores scope, so a PRIVATE report can join a PUBLIC ticket, and anonymous users see a public ticket's media (brief I.1.C); appending would publish the private reporter's photos. The brief (I.2 Obs.) groups by category/situation and 10 m only and is silent on scope and media, so this is the authors' reading.
- **Scope: the shared ticket keeps the creator's scope.** Grouping still ignores scope (brief I.2 Obs.); a joiner's own choice never changes the shared ticket, and their media stay private to them and operators. This answers the "which author's `alcance` wins" question below. Who may reopen stays with D-008 (decision 0011: creator and followers).
- **"Check against cached data": done by F-012.** The grouping query on `ticketLocations` answers from the local cache offline, so a match against a ticket this device has never loaded is missed and a second ticket is created. The join itself is idempotent (`arrayUnion`, and a reporter who already takes part gets `ALREADY_FOLLOWING`).

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
- A follower's own attachments and description are not separate tickets; their extra media is attached to the shared ticket (`Anexo.carregado por`). (Superseded by F-012: the media stay in the follower's `reports/{uid}` document; see the amendment above.)
- Per-citizen reopening and private/public scope apply to the shared ticket; open question for planning: whether a follower can reopen it, and which author's `alcance` (public/private) wins.
- Anonymous users still never see author data or exact coordinates.
- If a future requirement needs per-citizen tickets, Option B can be adopted by adding `Grupo`; the diagrams are kept for the report.
