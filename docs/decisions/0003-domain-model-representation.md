# 0003: Domain model representation in Kotlin

Status: accepted (D-001, Gate 1 on 2026-10-27)

## Context
Every phase-1 task (F-002..F-006, D-002..D-004) compiles against the domain types of the Option A diagram ([decision 0002](0002-grouped-reports-single-ticket.md)). The types must work now with sample data and previews, and later with Firestore (offline persistence), without changing their shape. Constraints: minSdk 24; the rule that anonymous users never see the author's personal data or exact coordinates; and both authors must be able to explain every file at the defence.

## Decision
- **Pure Kotlin model.** `model/` has no Android, Compose, Firebase or serialization imports. Business rules (D-002) are plain functions over these types.
- **English names, one type per file.** `User`, `Citizen`, `Operator`, `Municipality`, `Category`, `Ticket`, `Message`, `StatusHistory`, `Attachment`, `Coordinates`, and the enums `TicketStatus`, `Situation`, `Scope`, `AttachmentType`, `UploadState`, `RegistrationStatus` (mapping table in `docs/architecture/overview.md`).
- **`sealed interface User` instead of a `role` field.** `Citizen` and `Operator` are data classes that implement it, so `when (user)` is exhaustive and the type is the role. Rejected: an `open class User` (data classes extend it badly: duplicated parameters, `equals` pitfalls) and a single `User(role = ...)`. Firestore does not handle inheritance, so the data layer writes a `role` string in the document and uses it to choose `Citizen` or `Operator` when reading.
- **`String` ids, default `""`, and references by id.** `Ticket.creatorId`, `Ticket.followerIds`, `Message.authorId`, `StatusHistory.responsibleId`, `Attachment.uploadedById`. Embedding a `Citizen` would copy name, NIF, contacts and home coordinates into every ticket; Firestore cannot hide fields inside a document. Ids also suit `whereArrayContains` queries and keep documents small. `followerIds` does not include the creator; notifications go to `creatorId + followerIds`.
- **Category as `categoryId` + `categoryName` snapshot.** The list and card show the category offline without a lookup (phase 1 has no category source), and renaming a category does not rewrite old tickets. Rejected: embedding `Category` (copies `active`, goes stale) and `categoryId` only (lookup everywhere).
- **Time as `Long` epoch milliseconds, UTC, default `0L`.** Rejected: `com.google.firebase.Timestamp` (Firebase type in `model/`), `java.time.Instant` (API 26 or desugaring with minSdk 24, not mapped by Firestore), `java.util.Date` (mutable, clumsier maths). `Long` is immutable and makes the 5-day reopen and average repair time a subtraction. The `0L` default keeps previews deterministic; callers set the real time.
- **Enums persisted by constant name.** The names are a persisted contract and must not be renamed. Labels live in string resources and are mapped in the UI.
- **`Scope` defaults to `PRIVATE`.** A ticket that forgets to set its scope is never shown to anonymous users (privacy by default).
- **No-arg construction, `val` properties, `List` collections.** Every parameter has a default, so `Ticket()`, `Citizen()` and the rest build with no arguments (previews, sample data, and Firestore's no-arg constructor requirement). Properties are `val` so `UiState` holds immutable values. Firestore's `toObject()` cannot fill `val`s, so reading goes through a mapping step in `data/` (by hand from the document map, or small `var` DTOs) in the Firebase phase.
- **Non-null `Ticket.location`** (the brief makes location mandatory) and **`Attachment.file` as a `String`** (local content URI before upload, Storage path or URL after it; `android.net.Uri` is not allowed in `model/`).
- **Diagram associations not in the phase-1 spec are left out for now:** `define` (Municipio -> Categoria), `pertence a` (Operador -> Municipio), `valida` (Operador -> Municipe) and `gere` (Operador -> Ticket). Phase 1 has one municipality; they are added in the Firebase/operator tasks.

## Consequences
- Renaming a property or enum constant is now a cross-branch change (Francisco's F-tasks compile against these names).
- Default instances are not valid domain objects: ids are `""` (sample data must set distinct ids, lists key on `id`), `Coordinates()` is (0, 0), which is not a real place, and a default `Ticket()` has no history entry and no attachment, breaking the diagram's `1..*`. Creation logic enforces the invariants; distance code must not treat (0, 0) as a match.
- Sample data must set `scope = Scope.PUBLIC` on tickets meant for the anonymous list, or they disappear from it.
- The UI never shows `.name` of an enum or a raw epoch value: it maps enums to `stringResource` (EN + `values-pt`) and formats times with a locale-aware formatter.
- The model alone does not enforce the anonymous rule. `Ticket.location`, `creatorId`, `followerIds`, `messages` and `Citizen` personal data must be filtered in the repository layer, later with a public reduced-field collection.
- The Firebase phase adds: a `role` field, mapping code in `data/`, possibly subcollections for messages and history, and the four unmapped associations.
