# F-010: Firebase setup and Firestore schema
Branch: dev/Francisco · Base: d9a4d02
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-10 | feature (`/feature-dev`), task file `docs/tasks/phase-2/F-010-firebase-setup.md` |
| Plan | done | 2026-11-10 | 00-plan, 01-coding-plan, 03-docs-plan; 3 slices; 12 open questions; schema to agree with Diogo before Gate 1 |
| Gate 1: plan approved | done | 2026-11-10 | approved with all recommended defaults Q1-Q12; person confirmed Diogo agreed the schema |
| Code | pending | 2026-11-10 | slice 3 done (8b47917); slices 1-2 wait for `app/google-services.json` |
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
- 2026-11-10 (Code, slice 3): **deviation: slice 3 done before slices 1-2**, because `app/google-services.json` is not present yet and the build fails once the google-services plugin is applied. Slice 3 adds no Gradle or Kotlin changes, so it does not need the file.
  - Files: `firestore.rules`, `storage.rules`, `firebase.json` (no project id), `docs/decisions/0010-firestore-schema.md`, `docs/decisions/0011-firebase-plan-notifications-close-maps.md` (Q12). Commit 8b47917 `docs(F-010): draft security rules and schema decision` (message as given by the person's instruction, instead of the coding plan's `docs(F-010): draft Firestore and Storage rules and decision 0010`).
  - Rules follow the Gate 1 defaults: anonymous read only on `publicTickets`, `categories`, `municipality`; `ticketLocations` readable only by signed-in active citizens and operators (Q8); `participantIds` (Q9); `stats/summary` reserved and denied until F-011 (Q11); Storage paths without user ids, public media via `firestore.exists` on `publicTickets`.
  - Detail filled in beyond the plan's bullet list (draft, for later tasks to tighten): a lifecycle `update` for operators **and participants** limited to `currentStatus`, `statusHistory`, `attachments` with a non-shrinking history (needed by Q4's close-on-read and the F-013 upload state); `users` create checks the `nifs/{ROLE}_{nif}` doc with `getAfter` and the auth email; `tickets` create requires `ticketLocations` in the same batch and `publicTickets` iff scope is PUBLIC; `publicTickets` keys whitelisted; Storage uploads limited to `image/*`/`video/*` content types (no size limit yet, F-013).
  - Verify: `./gradlew assembleDebug lint` passed (no code change). `git check-ignore -v app/google-services.json` matches `.gitignore:20`; the file is not listed by `git status`. No project id or key in the rules or `firebase.json`. Each schema-table row has a `match` block. No `firebase deploy`, no Firebase CLI command run. Playground check: not run yet (the person).
