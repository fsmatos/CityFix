# F-002: StatusBadge and TicketCard
Branch: dev/Francisco · Base: a8be64a
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-10-28 | |
| Plan | done | 2026-10-28 | 3 slices; open questions Q1-Q6 with defaults |
| Gate 1: plan approved | done | 2026-10-28 | approved after brief fixes and @StringRes removal; defaults Q1-Q5 |
| Code | pending | 2026-10-28 | slice 1/3 done |
| Review | | | |
| Docs | | | |
| Pushed | | | |
| Merged | | | |

## Log
Append-only.
- 2026-10-28: merged origin/main into dev/Francisco first (a8be64a, brings D-001 domain model; no conflicts; assembleDebug + lint pass). Started F-002 from `docs/tasks/phase-1/F-002-status-badge-ticket-card.md`; dependencies D-001 and F-001 are both in the branch.
- 2026-10-28: Plan written (`00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`) from the task file, the phase-1 index and the neighbouring tasks (F-003, F-005, D-002, D-005, D-006). Three slices: StatusBadge + status labels; TicketCard collapsed with vector placeholder thumbnail; expanded details (description, situation, date) + `utils/formatDate`. `isExpanded` read as the global top-bar toggle (D-005/D-006). No new dependencies (material3 in the current BOM does not ship the icons artifact, so the placeholder is a vector drawable). Open questions Q1-Q6 (badge colours incl. optional `CityFixGreen`, expanded content, `isExpanded` meaning, placeholder, date format, label wording) each have a default. Awaiting Gate 1.
- 2026-10-28: Gate 1 decision: revise (brief checked via pdftotext). Corrections applied to the plan files: (1) PT status labels now match brief sections II and III: Aberto, Pedido de informações, Em resolução, Resolvido, Encerrado (EN unchanged); (2) PT situation labels Dano, Avaria, Destruição, Roubo confirmed by brief I.2.E, Q6 marked verified; (3) brief I.1.B: the collapsed card shows thumbnail + category + situation + StatusBadge, expanded adds only description and date. Situation strings and `situationLabelRes` moved from slice 3 to slice 2; slice 3 keeps description, `ticket_reported_on`, `utils/DateFormatting.kt` and the expanded previews; commit messages for slices 2-3 renamed. Q2 default, Outcome, acceptance checks, 01-coding-plan and 03-docs-plan updated. No code changed. Gate 1 still pending.
- 2026-10-28: Plan revised at the person's request: removed the `@StringRes` annotation from `statusLabelRes` and `situationLabelRes` (not used in the course material); both still return the `R.string` id as a plain `Int`. Gate 1 still pending.
- 2026-10-28: Gate 1 approved by the person with the revised plan (brief corrections, no `@StringRes`) and the defaults for Q1-Q5 (Q6 verified against the brief).
- 2026-10-28: Code slice 1 (StatusBadge): added the five `status_*` strings (EN + PT, PT wording from the brief), `CityFixGreen` in `Color.kt` (Q1 default), `ui/components/StatusBadge.kt` with `statusLabelRes` (plain `Int`, exhaustive `when`), `StatusBadge` (Surface + Text, Q1 colours) and the private `StatusBadgePreview` (portrait + landscape `spec:` device). Removed `ui/components/.gitkeep`. Verify: `assembleDebug` pass; `lint` pass, 10 warnings, all from earlier work (7 UnusedResources template colours, GradleDependency, NewerVersionAvailable, RedundantLabel), none from F-002, no `MissingTranslation`. grep `\.name` in `ui/components/` empty. No deviation.
