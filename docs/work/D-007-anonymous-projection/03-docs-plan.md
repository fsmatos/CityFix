# D-007: Docs plan

## Docs to update
- **`docs/decisions/0006-anonymous-projection-phase-1.md`** (Diogo's record; the task requires it): add a dated block "Amended 2026-11-10 (D-007)" and a pointer in the Status line.
  - The projection now also blanks `description` and clears `statusHistory`, keeping `currentStatus` (F-005 review S4, decision 0007). Per-entry blanking of `responsibleId`/`justification` is gone because the whole history is dropped.
  - Kept fields and why, including `createdAt` (ordering, never shown; Gate 1 Q2).
  - The expand toggle: Gate 1 Q1 choice and the rejected options (B: PUBLIC expanded card; C: zeroing `createdAt`).
  - The public projection contract for F-011: the table and the rules from `00-plan.md` (a view, not a valid ticket; no stats, lifecycle or grouping from it; rewrite on change; missing fields read as defaults).
  - Known limits left to other tasks (Q7): `CAPTURE_<millis>` file names, EXIF make/model/time, video and XMP location.
  - Update the Consequences line "registered citizens and operators need a separate, unprojected source": they also need it for the description and history now.
- **`docs/decisions/0007-ticket-detail-by-audience.md`** (Francisco's record): only if Gate 1 Q6 allows it, one line in Status/Consequences: "S4 done by D-007, see the amended 0006". Otherwise tell Francisco.
- **`docs/architecture/overview.md`:**
  - "Domain model" representation rules: the list of fields an anonymous view never shows gains `description` and `statusHistory`.
  - "ViewModel (D-003)": the `toAnonymousView()` bullet list (new fields, no per-entry history mapping); replace the "Projection gap found by F-005" and "Handover to Diogo (F-008)" items with a short "Done by D-007" note and a link to the contract in 0006.
  - "Top bar (D-005)": the controls table's Toggle column for LIST becomes "if `showExpandToggle` (never in phase 1)"; mention the new required parameter and the new preview.
  - "App shell (D-006)": `isExpanded` row says the cards are forced collapsed while `canExpandCards` is false; replace the "Open privacy work: D-007" bullet with what D-007 did; note the D-006 expanded-rotation check can no longer run on a device until roles exist.
  - "Shared components" (`TicketCard` anonymous caveat) and "Screens" (`ListScreen` open handover, `ShowScreen` privacy line about "the two fields the projection does not yet drop"): mark closed by D-007, without changing what the components do.
  - Handover to Francisco: `ShowScreen.previewTicket`'s comment "Already in anonymous form" is no longer accurate (it has a description and history); harmless, his file (Gate 1 Q5).
  - Handover to Francisco (F-011): point to the contract in 0006.
- **`docs/tasks/phase-1/D-007-anonymous-projection.md`:** tick the acceptance checks that were verified (the emulator ones only if Diogo confirms them), set the status.
- **`docs/tasks/phase-1/README.md`** and **`docs/tasks/phase-2/README.md`:** D-007 row status, if those tables track status.
- **`docs/report/notes.md`:** add a D-007 section (below); fix the stale lines that say the gap is open (the D-006 "Known gaps" bullets and the F-005 S4/list-consistency bullets) by marking them closed by D-007, keeping their history.

## Decisions to record
- No new decision record: decision 0006 is amended as the task requires. The toggle choice is recorded in that amendment, since it exists only because of the projection rule.

## Report notes
- **Privacy in layers, now complete for phase 1:** the list filter (`publicActive()`) picks which tickets; the projection (`toAnonymousView()`) picks which fields, now including description and history; the UI shows only the I.1.B/I.1.C fields. The third layer no longer does the second's job.
- **Why `currentStatus` survives without the history:** it is a denormalised copy (decision 0004), which pays off here: the badge needs no history.
- **Why the dashboard does not change:** stats are computed from the raw tickets inside the repository and leave it as aggregate counts, so no per-ticket data is exposed and clearing the history in the projection cannot affect them.
- **Toggle:** hidden for anonymous users rather than given an empty expanded state; the flag is the hook for the role check in the Auth task.
- **Why `createdAt` stays in the projection:** ordering in Firestore; it is not shown, and it is not the author's personal data.
- **The projection as the public document:** the same pure function builds the Firestore `publicTickets` document (F-011), so phase 1 and phase 2 share one rule. Firestore cannot hide fields inside a document, so a separate collection is the only way.
- **Improvement opportunities:** a separate `PublicTicket` type would prove the rule at compile time (rejected for now in 0006); round `createdAt` to the day if dates ever become sensitive; neutral camera file names; strip video and XMP location (media task).
