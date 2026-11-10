# F-010: Firebase setup and Firestore schema
Branch: dev/Francisco · Base: d9a4d02
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-10 | feature (`/feature-dev`), task file `docs/tasks/phase-2/F-010-firebase-setup.md` |
| Plan | done | 2026-11-10 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices; 12 open questions; schema to agree with Diogo before Gate 1 |
| Gate 1: plan approved | done | 2026-11-10 | approved with all recommended defaults Q1-Q12; person confirmed Diogo agreed the schema |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-10: fetched origin; origin/main had 1 commit missing from dev/Francisco (03b7327, merge of dev/Francisco into main, no file changes). The person chose to merge: d9a4d02 `merge: origin/main into dev/Francisco`, no conflicts, identical tree, so verify was not re-run. Started F-010 from the task file. `app/google-services.json` is not in the repo yet and is already listed in `.gitignore`.
- 2026-11-10 (Plan): wrote `00-plan.md`, `01-coding-plan.md` and `03-docs-plan.md`.
  - **Inputs read:** the task file, the phase-2 and phase-1 READMEs, decisions 0002/0003/0004/0009, the overview, `model/`, `CityFixApplication`, the version catalog and both Gradle files.
  - **Brief not read directly:** `docs/brief/AMov.ProjetoKotlin_PT.pdf` could not be rendered (no `pdftoppm`), so brief III/IV come from the task files' summaries (00-plan R6: re-read before Gate 1).
  - **Versions checked on the web:** Firebase BoM 35.1.0 (Google Maven metadata + Firebase release notes), google-services 4.5.0 (Google Maven metadata + Firebase setup page), kotlinx-coroutines-play-services 1.11.0 (Maven Central metadata).
  - **Plan finding:** Cloud Storage requires Blaze. Default buckets need Blaze since 2024-10-30, and Spark lost bucket access on 2026-02-03 (Firebase storage FAQ). So phase-2 decision 1 (Q1) recommends Blaze with a budget alert.
  - **Slices:**
    1. build deps + plugin (needs `google-services.json` locally);
    2. Firebase instances in `CityFixApplication` + persistent cache (smoke test run, then reverted);
    3. rules drafts + `firebase.json` + decision 0010 (no deploy).
  - **Open questions:** Q1-Q12, with recommended defaults, including the four phase-2 decisions (Q1, Q3, Q4, Q6), json handling (Q2), Firestore start mode/rules deploy (Q5), the MFA factor (Q7), and exact locations for 10 m grouping (Q8: new `ticketLocations` collection).
  - **Next:** schema (00-plan "Schema") to agree with Diogo before Gate 1.
- 2026-11-10: Gate 1: approved with defaults. The person chose "Diogo has agreed the schema; accept all recommended answers (Q1-Q12)": Q1 Blaze + budget alert, Q2 google-services.json stays ignored and is shared privately (D-015 copies it into the ZIP), Q3 notifications without Cloud Functions, Q4 5-day close computed on read, Q5 test mode until rules are deployed with the person's OK, Q6 osmdroid, Q7 SMS MFA with test numbers, Q8 `ticketLocations`, Q9 `participantIds`, Q10 no `AttachmentPhase` in F-010, Q11 reserve `stats/summary`, Q12 Gate decisions in decision 0011.
