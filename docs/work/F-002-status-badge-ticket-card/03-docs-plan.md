## Docs to update
- `docs/architecture/overview.md`, "UI foundation": add a short "Shared components" note: `ui/components/StatusBadge` (one colour + localised label per `TicketStatus`, colour mapping per Q1) and `TicketCard` (stateless; collapsed shows thumbnail, category, situation and status as brief I.1.B asks for the anonymous list, expanded adds description and date; `isExpanded` is the global top-bar toggle; reads only public ticket fields, so it is safe in the anonymous list); enum labels through `statusLabelRes` / `situationLabelRes` (exhaustive `when`, never `.name`); dates through `utils/formatDate` (`DateFormat`, device locale). If `CityFixGreen` is added, mention it next to the palette.
- `docs/tasks/phase-1/F-002-status-badge-ticket-card.md`: tick the acceptance checks once verified, only if task files are tracked that way (same rule as F-001).
- `docs/work/F-002-status-badge-ticket-card/state.md`: phase rows and log.

## Decisions to record
- No ADR expected: the choices follow the course style and existing rules. Write one only if the person picks a non-default answer with lasting impact (e.g. Q3 `isExpanded` as window-size class instead of the global toggle, which changes D-005/D-006).

## Report notes
Add a "F-002: StatusBadge and TicketCard" section to `docs/report/notes.md`:
- Technical solutions: stateless Compose components (data in, events out) reused by list and detail; `Surface` + `Text` badge; M3 `Card(onClick)` for a large accessible touch target; vector-drawable placeholder thumbnail; locale-aware dates with `java.text.DateFormat` (minSdk 24 without desugaring).
- Options chosen: collapsed card content taken from brief I.1.B (category, situation, thumbnail) plus the status badge, PT labels taken verbatim from the brief (Pedido de informações, Encerrado, ...); global expand/collapse toggle instead of per-card state (survives rotation via `rememberSaveable` in D-006, keeps the card stateless); text label on every badge so status is not conveyed by colour only (accessibility); exhaustive `when` for enum labels so a new status cannot ship without a translation; card reads only public fields (privacy by design for anonymous users).
- Rejected: interactive chips for the badge, `material-icons-extended` for one icon, Coil before there are real images, labels from `enum.name`.
- Improvement opportunities: real thumbnails with Coil from `Attachment.thumbnail` once uploaded (placeholder while `uploadState != UPLOADED`); relative dates ("3 days ago"); status icons in addition to colour and text; custom container colours for a softer badge palette; dark-theme badge colours.
