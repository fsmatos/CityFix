# Technical report notes

Raw material for the PDF technical report: technical solutions, options chosen and rejected, improvement opportunities. One section per task; each points to the decision records it relies on.

## D-001: Domain model (classes and enums)
Decisions: [0002](../decisions/0002-grouped-reports-single-ticket.md), [0003](../decisions/0003-domain-model-representation.md), [0004](../decisions/0004-denormalised-current-status.md). Mapping table: `docs/architecture/overview.md`, "Code mapping".

### Option A in code
- One `Ticket` with `creatorId` and `followerIds`; no `Group` class anywhere (decision 0002).
- A report with the same category + situation within 10 m of an active ticket adds the reporter to `followerIds` instead of creating a ticket. The check (D-002) compares `categoryId`, `situation` and `location`, ignores CLOSED tickets and must not count the default (0, 0) location as a match.
- `followerIds` does not contain the creator. Notifications (status change, info request, 20 m geofence, 24 h before closing) go to `creatorId + followerIds`.
- A follower's extra media is attached to the shared ticket with `Attachment.uploadedById`.
- Option B (one ticket per citizen plus `Grupo`) is kept as a diagram for comparison.

### Why `currentStatus` is duplicated
- It is a denormalised read model. The history (`statusHistory`) is the audit trail: the brief asks for full traceability of every transition with timestamps.
- `currentStatus` is what lists, the dashboard (% active, average repair time) and Firestore `whereEqualTo("currentStatus", ...)` queries read. Firestore cannot query the last element of an array or subcollection.
- Cost: one extra field per write. Gain: a list does not read every ticket's history.
- Risk: the two going out of sync. Handled by changing them only through one transition function (pure Kotlin) and one batched Firestore write.
- Transition times (RESOLVED for the 5-day reopen and 24 h alert; RESOLVED minus `createdAt` for average repair time) still come from the history.

### Inheritance without a role field
- `sealed interface User` with data classes `Citizen` and `Operator` keeps the diagram's specialisation of `Utilizador` and gives exhaustive `when (user)` in the UI and navigation.
- Rejected: `open class User` (data classes extend it badly) and a single `User(role = ...)`.
- Firestore does not handle inheritance: a `role` string is stored in the document and used to choose the class when reading.

### Privacy by design
- Users are referenced by id, so a ticket document never contains name, NIF, contacts or home coordinates.
- `Scope` defaults to `PRIVATE`: a ticket that forgets to set its scope is never public.
- Fields an anonymous view must never show: `Ticket.location` (coarse map zoom only), `creatorId`, `followerIds`, `messages`, and `Citizen` personal data including `georeferencing`. The model documents this; the repository layer enforces it.
- The 10 km radius of `Municipality` (Pr. Republica) limits where anonymous users may be, not where tickets may be.

### Category as id + name snapshot
- `Ticket.categoryId` + `Ticket.categoryName`. Lists show the category offline and without a lookup; an operator renaming a category does not rewrite old tickets.
- Rejected: embedding `Category` (stale copy of `active`), and the id alone (lookup everywhere; phase 1 has no category source).

### Pure model
- No Android or Firebase types in `model/`. The rules in D-002 (10 m grouping, 5-day reopen, dashboard stats) are plain Kotlin functions that are easy to explain and check by hand.
- Enums are stored by constant name; the names are a persisted contract. UI labels come from `strings.xml` (EN + `values-pt`), never from `.name`.

### Time as epoch millis
- `Long` epoch milliseconds, UTC, default `0L`.
- Beat `com.google.firebase.Timestamp` (Firebase in `model/`), `java.time.Instant` (needs API 26 or core-library desugaring with minSdk 24; Firestore does not map it) and `java.util.Date` (mutable, clumsier maths).
- Durations become subtractions (5 days = `5 * 24 * 60 * 60 * 1000L`). The UI formats with a locale-aware formatter (`DateFormat.getDateTimeInstance`).
- The `0L` default keeps previews deterministic (`Ticket() == Ticket()`).

### No-arg construction and immutability
- All parameters have defaults: previews and sample data in one line, and Firestore's no-arg constructor requirement is met.
- Trade-off: a default instance does not meet the diagram's `1..*` multiplicities (no history entry, no attachment), ids are `""` and coordinates are (0, 0). Creation logic enforces the invariants, and sample data sets distinct ids and real coordinates.
- `val` properties keep `UiState` immutable. Firestore's `toObject()` cannot fill `val`s, so the Firebase phase adds a mapping step in `data/`.

### Improvement opportunities
- Mark attachments as report media or resolution proof (RESOLVED requires proof).
- Store an FCM token per user.
- Use a public reduced-field ticket collection for anonymous users, so personal data and exact coordinates never reach their device.
- Use one document per NIF to guarantee NIF uniqueness.
- Move messages and history to subcollections when tickets grow (Firestore's 1 MB document limit); `StatusHistory` then needs an id.
- Add a `participantIds` field (creator + followers) so "my tickets" is one `whereArrayContains` query instead of two.
- Add the diagram associations left out in phase 1: category per municipality (`define`), operator municipality (`pertence a`), validated-by (`valida`), managed-by (`gere`).
- Use `FieldValue.arrayUnion` for `followerIds` so a repeated report never adds a duplicate follower.
- Show a placeholder on other devices until an attachment's `uploadState` is `UPLOADED` (before that, `file` is a local URI only the author's device can open).

## F-002: StatusBadge and TicketCard
No decision record (the choices follow the course style and existing rules). Details: `docs/architecture/overview.md`, "Shared components".

### Technical solutions
- Stateless Compose components (data in, events out) in `ui/components/`, reused by the list (F-003) and the detail screen (F-005); previewable with sample tickets in portrait, landscape and `locale = "pt"`.
- `StatusBadge`: `Surface` + `Text`, one colour per `TicketStatus` from the theme roles (primary, tertiary, secondary, inverse surface) plus `CityFixGreen` for RESOLVED.
- `TicketCard`: M3 `Card(onClick)`, which gives the whole card a large accessible touch target and the ripple for free. Collapsed: thumbnail, category, situation, badge. Expanded: adds description (max 3 lines) and date.
- Placeholder thumbnail: a vector drawable (Material "image" outline) tinted in Compose, so no `material-icons-extended` dependency for one icon.
- Dates: `java.text.DateFormat` (medium, device locale). Works on minSdk 24 without core-library desugaring; `createdAt == 0L` (unset) is skipped instead of showing 1 Jan 1970.

### Options chosen
- Collapsed content taken from brief I.1.B (category, situation, thumbnail) plus the status badge; expanded adds only description and date.
- PT labels verbatim from the brief: Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado; Dano, Avaria, Destruição, Roubo.
- One global expand/collapse toggle (top bar, D-005/D-006) instead of per-card state: the card stays stateless and the screen owns the flag (to survive rotation with `rememberSaveable`).
- Text label on every badge, so status is not conveyed by colour only (accessibility).
- Exhaustive `when` for enum labels (`statusLabelRes`, `situationLabelRes`), so a new constant cannot ship without a translation; never `enum.name`. The helpers return a plain `Int` without `@StringRes`, to match the course material.
- CLOSED badge in `inverseSurface`/`inverseOnSurface` and thumbnail box in `surface`: `surfaceVariant` (first choice) was almost the same as the card container and did not stand out (review S2).
- `CityFixGreen` with white text for RESOLVED: the only raw colour used outside the theme, because the M3 scheme has no green role.
- The card reads only `categoryName`, `situation`, `currentStatus`, `description` and `createdAt`: no location, author, followers or messages.

### Rejected
- Interactive chips (`AssistChip`, `FilterChip`) for the badge: they look clickable and add a touch target that does nothing.
- `material-icons-extended` for one placeholder icon (large dependency).
- Coil before there are real images to load.
- Labels from `enum.name` (not translatable; the names are a persisted contract).

### Improvement opportunities
- Real thumbnails with Coil from `Attachment.thumbnail` once uploaded (placeholder while `uploadState != UPLOADED`).
- Relative dates ("3 days ago").
- Status icons in addition to colour and text.
- Custom container colours for a softer badge palette, and dark-theme badge colours (with a green role in the theme instead of the raw `CityFixGreen`).
- Placeholder `contentDescription` that does not announce a photo when there is none (review N1).
