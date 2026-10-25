# CityFix

CityFix is an Android app (Kotlin, Jetpack Compose) for civic participation: citizens report, geolocate and follow urban issues; municipal operators manage the ticket lifecycle. It has three roles (anonymous citizen within 10 km of Pr. Republica, registered citizen, municipal operator) and runs on Firebase (Auth with MFA, Firestore offline, Storage, FCM) with WorkManager uploads, a 20 m geofence and vertical-impact detection. It is a university assignment (ISEC, Arquiteturas Moveis 2026/27); the requirements are in `docs/brief/AMov.ProjetoKotlin_PT.pdf`.

**Deadline: 2026-11-22 23:59** (-5% per hour late). Defence 23-27 Nov; both authors must be able to explain every file.

## Architecture map
- Single Gradle module `:app`, package `pt.isec.amov.cityfix`, minSdk 24, targetSdk 37. Version catalog: `gradle/libs.versions.toml`.
- MVVM with stateless/stateful composables. Folders: `ui/` (`screens/`, `components/`, `navigation/`, `theme/`), `viewmodel/`, `data/` (repositories, Firebase, workers), `model/` (entities, enums and pure rules), `utils/` (helpers such as image compression and distance maths).
- Domain model (Option A: one ticket, several following citizens, no `Grupo`): `docs/architecture/diagrams/CityFix_Dominio_OpcaoA.png`, decision `docs/decisions/0002-grouped-reports-single-ticket.md`.
- Details: `docs/architecture/overview.md`; decisions: `docs/decisions/`.

## Commands
- Build: `./gradlew assembleDebug`
- Lint: `./gradlew lint`
- No automated tests (not required for this assignment). No formatter or CI is configured.

## Workflow
- Tasks are Markdown files the person puts in `docs/tasks/`; IDs are per person: first letter of your dev branch + counter (`F-001` for `dev/Francisco`, `D-001` for `dev/Diogo`). No tracker.
- Each person works directly on their own branch: `dev/Francisco` or `dev/Diogo`. No per-task branches, no worktrees. `main` is the integration branch.
- Plans live in `docs/work/<ID>-<slug>/` (`00-plan`, `01-coding-plan`, `03-docs-plan`, `state.md`, `review.md`).
- **Gate 1** (only gate): the person approves the plan before any code.
- `/push-task` pushes your own dev branch (no PR). `/merge` brings `origin/main` into your dev branch; merging a dev branch into `main` happens only when the person asks.
- Commits: Conventional Commits with task ID, e.g. `feat(F-001): add login screen`.
- Fresh clone: run `git config core.hooksPath .githooks` once.

## Non-negotiable rules
- Never merge into or push `main` unless the person asks in this conversation.
- Never skip Gate 1; record deviations from the plan in `state.md`.
- No attribution in commits or PRs: no `Co-Authored-By: Claude`, no `Claude-Session:`, no "Generated with Claude Code".
- Never `git push --force`; never rewrite `main`'s history.
- Ask before `firebase deploy`, changing Firestore/Storage rules, or touching `google-services.json` or keys; never commit secrets.
- Anonymous users must never see the author's personal data or exact coordinates.
- Delivery ZIP `AMOV2627.<nr1>.<nr2>` must exclude `.gradle`, `build`, `app/build`, `local.properties`, and include the PDF technical report.

## Commands cheat sheet
| Command | Purpose |
| --- | --- |
| `/feature-dev [task]` | Full flow: start, plan, Gate 1, code, review, docs |
| `/bug-fix <task>` | Same flow for bugs |
| `/refactor <desc>` | Same flow, behavior unchanged |
| `/plan-feature`, `/plan-fix` | Planner + Gate 1 |
| `/code [id] [slice]` | Implement the approved plan |
| `/review-code`, `/write-docs` | Single phases |
| `/start-task`, `/push-task`, `/merge`, `/finish-task` | Git commands |
| `/work-status` | Where every task stands |

Agents: planner, implementer-kotlin, reviewer, docs-writer, merge-resolver (`.claude/agents/`). Skills `kotlin-conventions`, `work-item`, `git-workflow` are background knowledge; `verify` runs before commits.
