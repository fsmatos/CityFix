# D-001: Docs plan

## Docs to update
- `docs/architecture/overview.md`, section "Domain model": add a short "Code mapping" paragraph under the Portuguese entity list.
  - English names: Utilizador -> `User`, Municipe -> `Citizen`, Operador -> `Operator`, Municipio -> `Municipality`, Categoria -> `Category`, Mensagem -> `Message`, HistoricoEstado -> `StatusHistory`, Anexo -> `Attachment`. Enums: `TicketStatus`, `Situation`, `Scope`, `AttachmentType`, `UploadState`, `RegistrationStatus`.
  - Representation rules: `String` ids, references by id, time as epoch millis, enums persisted by name, `User` as a sealed interface, every class buildable with no arguments.
  - The association mapping table from `00-plan.md` (Architecture impact).
- `docs/tasks/phase-1/README.md`: no change by the docs-writer. If Q1 is answered differently from the recommendation, tell the person so Francisco's tasks can be checked.

## Decisions to record
- `docs/decisions/0003-domain-model-representation.md` (new; context, decision, consequences):
  - Pure Kotlin model with no Firebase types.
  - `sealed interface User` instead of a `role` field; the data layer writes `role` in Firestore.
  - `String` ids, and references by id (privacy: no personal data copied into tickets).
  - `Long` epoch millis instead of `Timestamp`/`Date`/`Instant` (minSdk 24, purity, arithmetic).
  - Enums stored by name (the constant names are a contract).
  - `val` + a later mapping step in `data/` (Q6).
  - Category reference + name snapshot (Q1).
  - Record whatever the person chose at Gate 1 for Q1-Q8.
- `docs/decisions/0004-denormalised-current-status.md` (new, or a section of 0003 if the person prefers fewer files): `Ticket.currentStatus` is kept next to `statusHistory`.
  - Context: list, dashboard and filters need the current state; Firestore cannot query the last element of an array or subcollection.
  - Decision: keep the field.
  - Consequence: every transition appends history and sets `currentStatus` in the same write (a pure helper plus a Firestore batch later).

## Report notes
For the PDF technical report:
- **Option A in code.** One `Ticket` with `creatorId` and `followerIds`; no `Group` class (decision 0002). A report with the same category + situation within 10 m adds the reporter to `followerIds`. Notifications go to the creator plus the followers.
- **Why `currentStatus` is duplicated.** It is a denormalised read model. The history is the audit trail (the brief asks for full traceability with timestamps), and `currentStatus` is what lists, the dashboard (% active, average repair time) and Firestore `whereEqualTo` queries read. It costs one extra field per write, and it removes reading every ticket's history to draw a list. The risk is the two going out of sync, which is handled by changing them only through one transition function and one batched write.
- **Inheritance without a role field.** `sealed interface User` with data classes `Citizen`/`Operator` keeps the diagram's specialisation and gives exhaustive `when` in the UI. Firestore does not handle inheritance, so a `role` string is stored in the document and used to choose the class when reading.
- **Privacy by design.** Users are referenced by id, so a ticket document never contains name, NIF, contacts or home coordinates. `Scope` defaults to `PRIVATE` (if adopted, Q3). This supports the rule that anonymous users never see author data or exact coordinates.
- **Pure model.** No Android/Firebase types in `model/`, so the rules in D-002 (10 m grouping, 5-day reopen, dashboard stats) are plain Kotlin functions that are easy to explain and check by hand.
- **Time as epoch millis.** Why it beat `Timestamp`, `Date` and `Instant` (minSdk 24 without desugaring, immutability, simple maths).
- **No-arg construction.** All defaults give previews and sample data in one line and satisfy Firestore's no-arg constructor requirement. The trade-off is that a default instance does not meet the diagram's `1..*` multiplicities, so creation logic enforces them.
- **Improvement opportunities:**
  - Mark attachments as report media or resolution proof (RESOLVED requires proof).
  - Store an FCM token per user.
  - Use a public reduced-field ticket collection for anonymous users.
  - Use one document per NIF for uniqueness.
  - Move messages and history to subcollections when tickets grow (Firestore's 1 MB document limit).
  - Add a `participantIds` field for "my tickets" queries.
  - Add the diagram associations left out in phase 1 (operator municipality, validated-by, managed-by).
