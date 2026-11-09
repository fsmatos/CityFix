# F-017: Technical report (PDF)

Owner: Francisco (`dev/Francisco`), with sections from Diogo · Layer: docs · Phase 2 · Status: to do · Target: 2026-11-21
Depends on: all coding tasks (start the outline on 11-18)

## Goal
Write the technical report the delivery ZIP must include.

## Why
- Brief VI: the PDF must state the technical solutions, clarify the implementation options taken and working, and identify improvement opportunities. Missing it costs marks; it is also the defence script.

## What to build
- Source: `docs/report/notes.md` (written per task), `docs/architecture/overview.md`, `docs/decisions/`, the domain diagrams.
- Suggested structure: 1. Introduction and group; 2. Architecture (MVVM, stateless/stateful, packages, repositories, diagram); 3. Data model and Firestore schema (Option A, public projection, NIF uniqueness); 4. Features by role, with screenshots in portrait and landscape and EN/PT; 5. Brief compliance table (each brief item: done / partial / not done, and where); 6. Technical solutions (offline, WorkManager, compression, location, grouping, geofence, sensors, notifications, MFA); 7. Options and limits (Spark vs Blaze, foreground-only detection, etc.); 8. Improvement opportunities; 9. Test accounts and how to run.
- Diogo writes the sections for his tasks (auth, registration, operator, geofence); Francisco assembles.
- Export to `docs/report/AMOV2627.2018013532.2018044613_Relatorio.pdf` (or the name you prefer).

## Acceptance checks
- [ ] Every brief section (I-V) appears in the compliance table.
- [ ] Screenshots of every main screen; test accounts listed (no real passwords of other services).
- [ ] PDF is in the ZIP (D-015).

## References
Brief VI; `docs/report/notes.md`.
