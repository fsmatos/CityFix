# Project Manifest: CityFix

Source of truth for regenerating the factory. Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`.

## Product
- **P1** CityFix: Android app (Kotlin) for civic participation. Citizens report, geolocate and follow urban issues; municipal operators manage them.
- **P2** Anonymous citizen (within 10 km of Pr. Republica), registered citizen, municipal operator. First real user: ISEC lecturer and grader.
- **P3** v1 is the whole brief: 3 roles with permissions; registration, MFA login, profile edit; issue CRUD with media; automatic grouping of reports within 10 m of the same type/situation; ticket lifecycle (Open, Info request, In progress, Resolved, Closed; 5-day reopen window); FCM and local notifications; 20 m operator geofence; vertical-impact detection; dashboard; offline support; WorkManager uploads with local compression and thumbnails.
- **P4** Out of scope: management profile above the operator (operators register active immediately). Anything beyond the brief.
- **P5** Personal data (NIF, location, contacts) hidden from anonymous users. Deadline 2026-11-22 23:59 (-5% per hour late). Defence 23-27 Nov, both students present. Delivery: one ZIP `AMOV2627.<nr1>.<nr2>` without `.gradle`, `build`, `app/build`, `local.properties`, plus a PDF technical report. Default language English plus Portuguese. Portrait and landscape. Accessibility.

## Architecture
- **A1** One Android app. Kotlin, Jetpack Compose + Material3, MVVM with stateless/stateful composable split. minSdk 24, targetSdk 37. Package `pt.isec.amov.cityfix`.
- **A2** Single repo, single Gradle module `:app`, version catalog `gradle/libs.versions.toml`.
- **A3** Firebase Auth (+MFA), Cloud Firestore (offline persistence), Firebase Storage, FCM, WorkManager uploads, geofencing and location (foreground and background), sensors for impact detection.
- **A4** Brief PDF in `docs/brief/`.

## Workflow
- **W1** No tracker. Tasks are Markdown files the person provides in `docs/tasks/`. IDs per person: first letter of the dev branch + counter (`F-001` for `dev/Francisco`, `D-001` for `dev/Diogo`).
- **W2** UI design: none.
- **W3** Gate 1 only: plan approved before code. No Gate 2.
- **W4** `by-hand` label; no exams.
- **W5** Each person works directly on their own branch `dev/Francisco` or `dev/Diogo`; no per-task branches. Integration branch `main`; remote `origin`.
- **W6** Always in place, on the person's dev branch. No worktrees.
- **W7** Push only (`git push -u origin dev/<name>`), no PR. Only your own dev branch.
- **W8** Merge commit `--no-ff`. `/merge` brings `origin/main` into the dev branch; dev into `main` only when the person asks. The factory never pushes `main` and never touches the other person's dev branch.
- **W9** Conventional Commits with task ID, e.g. `feat(F-001): add login screen`. No Claude attribution.

## Quality
- **Q1** Format: none. Lint: `./gradlew lint`. Test: none (no automated tests). Build: `./gradlew assembleDebug`. No coverage goal.
- **Q2** No CI.
- **Q3** Not applicable.
- **Q4** Ask first: `firebase deploy`, Firestore/Storage rules changes, anything touching `google-services.json` or keys. Denied: force-push, pushing `main`, `git reset --hard`, `rm -rf`.
- **Q5** Models: `inherit` everywhere.
