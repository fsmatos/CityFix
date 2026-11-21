# F-017: Technical report (PDF)
Branch: dev/Diogo · Base: a360c50
Remote: not pushed

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-21 | Task file `docs/tasks/phase-2/F-017-technical-report.md` (owner Francisco); Diogo decided to write the whole report on dev/Diogo, Francisco reviews |
| Plan | skipped | | Docs-only task; structure given by the task file and Diogo's instructions |
| Gate 1: plan approved | skipped | | No code |
| Code | skipped | | No code |
| Review | pending | | Francisco reviews the text |
| Docs | done | 2026-11-21 | `docs/report/relatorio.md` (pt-PT Markdown for marked.js) |
| Pushed | pending | | |
| Merged | pending | | |

## Log
- 2026-11-21 Docs: wrote `docs/report/relatorio.md` in pt-PT: cover, index, sections 1-10 (introduction, architecture, data model and Firestore schema with the Option A/B diagrams, features by role with screenshot placeholders, brief compliance table I-V, technical solutions, options and limits, improvements, test accounts and how to run, conclusion). Sources: brief PDF, `docs/report/notes.md`, `docs/architecture/overview.md`, `docs/decisions/`, D-016 state, `docs/testing/manual-test-checklist.md`; quoted UI texts checked against `values-pt/strings.xml`. Not committed (Diogo's instruction); no code touched.
- Deviation: the task file names Francisco as owner and assembler; Diogo wrote the report and Francisco reviews.
- Open for Diogo: Francisco's surname on the cover; screenshots for every `[Captura: ...]` line; test accounts table (e-mails, passwords, test numbers, codes; PDF only, never in git); operator test domain; test-mode expiry date (2026-12-10 or 2027-03-01); ZIP source SHA; export to `AMOV2627.2018013532.2018044613_Relatorio.pdf`.
- 2026-11-21: 27 screenshots taken on the emulator (20 and 29, the notifications, left out by Diogo); report moved into the ISEC Word template with scripts/build-report-docx.ps1 (HTML import into the template, numbered captions, indexes). Output: docs/report/AMOV2627.2018013532.2018044613_Relatorio.docx and .pdf (67 pages). Still to fill: test accounts (section 9.1).
