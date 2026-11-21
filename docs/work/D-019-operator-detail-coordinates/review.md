# D-019 review

Reviewed: `git diff e6b34f4..HEAD` (code commit 20a82b0) against `00-plan.md`, `01-coding-plan.md`,
Gate 1 answers in `state.md`, the task file, brief I.3.E, CLAUDE.md and the kotlin conventions.
Date: 2026-11-21. Reviewer did not rebuild; build/lint results are taken from `state.md`.

Verdict: **no blocking findings**. The code matches the coding plan step by step (steps 1-11), with
no unrecorded deviations.

## What was checked
- Plan conformance: watched ids now include `attachments.uploadedById` (OperatorTicketScreen:118);
  `responsibleNames` reaches `DetailsPane` in both orientations -> `MediaSection(uploaderNames)` ->
  `MediaTile(uploaderName)`; `ExactLocationSection(showCoordinates = audience == OPERATOR)`;
  `formatCoordinate` with `Locale.ROOT` and `%.5f`; KDocs updated incl. `watchPeople`; operator
  preview with uploaders, a resolution proof and a landscape preview; 3 strings in EN and PT.
- Gate 1: Q1 name kept (KDoc widened), Q2 citizen unchanged, Q3 dot decimal in both languages,
  Q4 "Added by" / "Adicionado por". All honoured.
- Privacy (CLAUDE.md): the coordinates line is gated on `DetailAudience.OPERATOR` only; names come
  only from `responsibleNames`, which only `OperatorTicketScreen` passes. `TicketDetailScreen`
  (REGISTERED) and the PUBLIC call in `MainActivity.kt:606` pass no map, so they render no names;
  the anonymous projection also blanks `uploadedById` (`TicketPrivacy.kt:43`) and coarsens the
  location. Ids are never rendered (map miss -> no line). Registered-citizen view is unchanged.
- MVVM / stateless: `ShowScreen` stays stateless (data in, no ViewModel); the profile lookup stays in
  `OperatorTicketViewModel.watchPeople`, which already skips blank ids and "system".
- Failure modes: (0, 0) keeps "Map not available" and skips the coordinates line (inside the
  `else`); unreadable profiles map to `null` in `toContact()` and are dropped by `mapNotNull`.
- i18n: both files have the 3 strings with positional args; no hard-coded user text (the `"%.5f"`
  pattern is not user-visible).
- Accessibility: the tile column already has `semantics(mergeDescendants = true)`, so "Added by" is
  read with the tile; the coordinates `Text` has its own content description.
- Orientation: both `DetailsPane` call sites pass the names; `maxLines = 2` + ellipsis keeps the
  120dp tile bounded; a landscape preview was added.

## Blocking
None.

## Should-fix (non-blocking)
1. **Manual acceptance checks are still open.** All boxes in `00-plan.md` are unticked, and the
   TalkBack, rotation and PT checks cannot be proven by build/lint. In particular confirm how PT
   TalkBack reads a dot decimal ("40.21000") in the `cd_ticket_coordinates` description.
   Fix: run the checklist on device (operator, citizen own ticket, anonymous; portrait/landscape;
   EN/PT; TalkBack on), tick the boxes in `00-plan.md` and note the result in the `state.md` Log
   before `/push-task`.
2. **Coordination on `ShowScreen.kt`.** It is Francisco's file (F-005) and the plan lists the
   conflict risk; `state.md` still has "tell Francisco ShowScreen.kt changed" as pending.
   Fix: tell him before pushing and record it in the Log.

## Nit
3. **Blank profile name renders "Added by " (and "By ").** `responsibleNames` keeps a profile whose
   `name` is blank, so the tile would show a dangling label. Same pre-existing behaviour in the
   history, so not a regression. Fix (one place, covers both): in `OperatorTicketScreen.kt:227`
   build the map with
   `contact?.name?.takeIf { it.isNotBlank() }?.let { id to it }`.
4. **Two names for the same map in one file** (`responsibleNames` in `ShowScreen`/`DetailsPane`,
   `uploaderNames` in `MediaSection`). Accepted at Gate 1 (Q1) and documented in the KDoc, but be
   ready to explain it in the defence. Fix (optional, after the deadline pressure): rename the
   public parameter to `peopleNames` in a separate refactor coordinated with Francisco.
