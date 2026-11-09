# F-011: Firestore tickets repository and public projection

Owner: Francisco (`dev/Francisco`) · Layer: `data` / `viewmodel` · Phase 2 · Status: to do · Target: 2026-11-13
Depends on: F-010, D-007 · Blocks: F-012, F-013, F-014, D-012, D-013, F-016

## Goal
Replace the in-memory data with Firestore behind the existing `TicketsRepository`, so tickets and categories are shared between users and work offline.

## Why
- Brief IV: tickets, categories and messages in Firestore with native offline cache.
- Decision 0009 made the repository so this swap does not touch the screens.
- CLAUDE.md: anonymous users never see personal data or exact coordinates; Firestore cannot hide fields, so they read `publicTickets` only (phase 1 risk 3).

## What to build
- **`data/FirestoreTicketsRepository`** implementing `TicketsRepository`. Move the read API from one-shot `suspend` loads to `Flow` (snapshot listeners) so lists update live; adjust `TicketsViewModel` (Diogo's file: tell him).
- **Audience-aware reads:** anonymous → `publicTickets`; citizen → their own and followed tickets plus public ones; operator → all tickets. Keep `toAnonymousView()` as the single function that builds a `publicTickets` document.
- **Writes:** `reportTicket` writes `tickets/{id}` and `publicTickets/{id}` in one batch (public scope only), with the creator id from Auth. Attachments keep their local URI with `uploadState = PENDING` (F-013 uploads them).
- **Categories** from `categories` (active only for the report form); a one-off seed of the sample categories (script or debug button, removed before delivery).
- **Dashboard stats** (I.1.A): computed from `publicTickets` plus a counter for private tickets, or a stats document; decide at Gate 1 (anonymous users cannot read private tickets).
- Keep `LocalTicketsRepository` for previews or delete it (decide at Gate 1).
- Update decision 0009 and the overview.

## Files
New `data/FirestoreTicketsRepository.kt`; `data/TicketsRepository.kt`, `model/TicketPrivacy.kt`, `viewmodel/TicketsViewModel.kt`, `viewmodel/ReportViewModel.kt`, `CityFixApplication.kt`, docs.

## Acceptance checks
- [ ] A report made on one device appears on another.
- [ ] In airplane mode the list still loads and a new report is queued and syncs later.
- [ ] Logged out, no Firestore read touches `tickets/`; the anonymous list shows category, situation and thumbnail only.
- [ ] Dashboard: average repair time, total tickets, % active.
- [ ] `assembleDebug` and `lint` pass.

## Out of scope
Location and grouping (F-012), uploads (F-013), messages (F-014), operator writes (D-013).

## References
Brief I.1, IV; decisions 0005, 0006, 0007, 0009, 0010.
