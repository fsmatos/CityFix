# D-001: Review

Reviewed: `git diff a280ab1..HEAD` (commits 922c382, 811fe11, 2cc2df2, fffc6e1, 9c8835e, 760bc74), 16 files in `app/src/main/java/pt/isec/amov/cityfix/model/` plus `state.md`.
Checked against: `00-plan.md`, `01-coding-plan.md`, the Gate 1 decisions in the `state.md` Log, `docs/tasks/phase-1/D-001-model-classes.md` and the phase-1 README, the brief (sections I, II, IV, V), `docs/architecture/overview.md`, decision 0002, the Option A diagram and the kotlin-conventions skill.
Date: 2026-10-27 · Reviewer: reviewer agent

## Verdict
**Approve. No blocking findings.** The code matches the Interfaces block of `01-coding-plan.md` field for field, and it applies the Gate 1 decisions (`categoryId` + `categoryName`, `Scope.PRIVATE`, no Q4 links, `Long` times, non-null `location`, `val`, creator not in `followerIds`, `file` as String). The two should-fix items are KDoc changes. They are cheap and worth doing before `dev/Diogo` is pushed to Francisco, because the KDoc is the only privacy guard phase 1 has.

## What was verified
- `./gradlew assembleDebug` passes on HEAD (I re-ran it during this review).
- No file in `model/` has an `import`. There is no Android, Firebase, Compose or serialization code, and no `R`.
- `grep -rniE "class (Group|Grupo)" app/src` finds nothing. `Ticket` has `creatorId` plus `followerIds` (Option A, decision 0002).
- `User` is a `sealed interface` and `Citizen` and `Operator` are `data class`es. There is no `role` field.
- Enum constants match the task spec exactly: `TicketStatus`, `Situation`, `Scope`. `UploadState` and `RegistrationStatus` match the diagram.
- Every constructor parameter has a default, so each class can be built with no arguments. All properties are `val` and the collections are `List`.
- `Ticket.currentStatus` has a KDoc giving the reason (lists, filters, queries). The reason is also in `03-docs-plan.md` (Report notes).
- One type per file, with trailing commas.
- The commit messages are Conventional Commits with the task ID and have no attribution lines.
- Brief cross-check, all supported:
  - The anonymous list needs category, situation and thumbnail: `categoryName`, `situation`, `attachments[].thumbnail`.
  - The dashboard computes average repair time from `createdAt` and the RESOLVED `StatusHistory.dateTime`.
  - The 5-day reopen and the 24 h alert use the RESOLVED entry.
  - Reopening with a reason uses `StatusHistory.justification`.
  - A follower's extra media uses `Attachment.uploadedById`.
- Not applicable to this diff:
  - Runtime permissions, i18n resources, accessibility, orientation, and MVVM stateless/stateful split. `model/` has no UI and no user-facing strings, which is correct.
  - The i18n consequence for consumers is listed below (C4).

## Consumer check (F-002..F-006, D-002..D-006)
| Task | Needs | Provided | Status |
| --- | --- | --- | --- |
| F-002 `StatusBadge(status)`, `TicketCard(ticket, ...)` | `TicketStatus` (5 values), category label, thumbnail | `TicketStatus`, `Ticket.categoryName`, `attachments.firstOrNull()?.thumbnail` | OK |
| F-003 `ListScreen`, `key = { it.id }` | `Ticket.id: String` | yes | OK (sample ids must be distinct, KDoc says so) |
| F-004 `DashboardScreen(stats)` | via `DashboardStats` (D-002) | n/a | OK |
| F-005 `ShowScreen(ticket?)` | history in order with timestamps; hide author and coordinates | `statusHistory[].status/dateTime`; author is only `creatorId` | OK, see S2 |
| F-006 `ReportScreen` | category, situation, description, scope via enums | `Category`, `Situation`, `Scope`, `description` | OK, see C3 |
| D-002 `findNearby(location, category, situation)`, `publicActive()`, `stats()` | `location`, category key, `situation`, `scope`, `currentStatus`, `createdAt`, history | yes | OK, see C1, C2 |
| D-003 sample data, `getTicketById` | String id, no-arg construction | yes | OK, see C3 |
| D-004 `TicketDetailRoute(ticketId)` | id type | `String` | OK, see N5 |
| D-005, D-006 | nothing from `model/` | n/a | OK |

No consumer needs a field that is missing, and no name conflicts with the task files. I could not check `origin/dev/Francisco` for code written against guessed names because `git fetch` was declined at Start. The local `origin/dev/Francisco` ref has no model usage.

## Blocking
None.

## Should-fix (non-blocking)

**S1. `Municipality` KDoc misstates the 10 km rule** (`Municipality.kt:3`).
- The KDoc says "tickets lie within [radiusKm] of [center]".
- The brief (I.1) says the radius limits where *anonymous users* must be located (10 km from Pr. Republica). Tickets are not limited by it, and registered users can be anywhere.
- At the defence this line contradicts the brief, and D-002 or a later task could implement the wrong rule from it.
- Fix: change the KDoc to
  `/** Municipality ("Municipio" in the brief). Operators register with an e-mail in [emailDomain]; anonymous users must be within [radiusKm] of [center] (Pr. Republica, 10 km). */`

**S2. Personal and exact data on `Ticket` is not marked for the anonymous rule** (`Ticket.kt`).
- In phase 1, D-003 passes the whole `Ticket` to `ListScreen` and `ShowScreen`. Nothing in the model tells the UI which fields an anonymous user must never see.
- Brief I.1.C says the anonymous detail hides the author's personal data and georeferencing, and shows only images or videos and a map at coarse zoom. F-005 has an acceptance check for this.
- `Citizen.georeferencing` already carries this kind of warning, but `Ticket.location`, `creatorId`, `followerIds` and `messages` do not.
- Fix: add one-line KDocs:
  - `location`: `/** Exact report location. Anonymous views must not show it: coarse map zoom only (brief I.1.C). */`
  - `creatorId` and `followerIds`: add "Not shown to anonymous users."
  - `messages`: `/** Thread between citizens and the municipality; registered participants and operators only. */`

  Optionally, add a line to the class KDoc that points to README risk 3 (a public reduced-field collection in the Firebase phase).

## Coordination notes for consuming tasks (non-blocking, not changes to D-001)
These are what the consumers need to know when they plan. Send them to Francisco together with the Interfaces block.

**C1. D-002 `findNearby`.**
- Take `categoryId: String`, not a `Category` object: the ticket stores only `categoryId` and `categoryName`.
- Match only tickets that are not CLOSED. Decision 0002 says "existing active ticket".
- Do not let a default `Coordinates()` (0, 0) count as a match.

**C2. D-002 `stats()`.**
- Average repair time is the RESOLVED `StatusHistory.dateTime` minus `createdAt`.
- Skip tickets that have no RESOLVED entry. Use `firstOrNull`, never `first`, so a default `Ticket()` cannot throw.
- Guard against zero tickets.

**C3. D-003 sample data.** Set the following on every sample:
- distinct `id`s
- real coordinates near Pr. Republica
- `categoryId` and `categoryName`
- at least one `Attachment` and one `StatusHistory` entry whose last status equals `currentStatus`
- `scope = Scope.PUBLIC` on any ticket that should appear in the anonymous list. The default is `PRIVATE`, so a sample that forgets this disappears from `publicActive()`.

Also expose a small list of active `Category`s for F-006's category picker. Phase 1 has no other category source.

**C4. F-002 and F-006 i18n.** Map `TicketStatus`, `Situation` and `Scope` to `stringResource(R.string....)` in the UI (EN default plus `values-pt`). Never show `.name` to the user. F-005 should format `dateTime` and `sentAt` with a locale-aware formatter (`java.text.DateFormat.getDateTimeInstance`), not raw epoch values.

## Nit (non-blocking)

**N1. `Attachment.file` across devices** (`Attachment.kt:8`).
- Before upload, the field holds a local content URI. Once Firestore syncs the ticket, other clients receive a URI they cannot open.
- Fix: add to the KDoc: "Other devices must show a placeholder until [uploadState] is `UPLOADED`."

**N2. `followerIds` uniqueness** (`Ticket.kt:29`).
- Add "No duplicates" to the KDoc. A creator or follower who reports the same issue again must not be appended. Later this is `FieldValue.arrayUnion`.

**N3. `StatusHistory` fields have no meaning in KDoc** (`StatusHistory.kt`).
- A one-liner each helps at the defence:
  - `justification`: "Reason for reopening or for an info request; null otherwise."
  - `responsibleId`: "User id of who made the transition (creator for the first OPEN entry)."

**N4. `Message` and `android.os.Message`.**
- The IDE's auto-import can offer `android.os.Message` in F-005 or D-003. No rename is needed, since the name is in the contract. Fix: when you first use it, check that the import is `pt.isec.amov.cityfix.model.Message`.

**N5. D-004 has no serialization plugin.**
- `gradle/libs.versions.toml` has no `kotlin-serialization` plugin, but D-004's spec says "from the version catalog". This is not a D-001 issue.
- Fix (in D-004): add the plugin and `kotlinx-serialization-json` to the catalog and declare `TicketDetailRoute(val ticketId: String)`.

**N6. Process.**
- `core.hooksPath` is not set in this clone (Log, Code phase), so the commit-msg hook is inactive. Run `git config core.hooksPath .githooks` before the next commit.
- `state.md`'s `Base: 522011d` is the start point. The code-only diff base is `a280ab1` (the plan commit). This is harmless, but reviewers should use `a280ab1..HEAD` to see code only.
