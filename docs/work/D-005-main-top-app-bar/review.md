# D-005: Review

Reviewer: reviewer agent · Date: 2026-10-31
Diff: `git diff 3b71343..HEAD` (cc266d8, db0ccbf, 408f001, bee7beb). `state.md` records Base d910a26. The range d910a26..3b71343 only adds this task's plan and `state.md`, so this review leaves it out.
Checked against: 00-plan, 01-coding-plan, 03-docs-plan, task file `docs/tasks/phase-1/D-005-main-top-app-bar.md`, overview.md, kotlin-conventions, `.claude/rules/kotlin.md`, CLAUDE.md, the brief (text extracted with `pdftotext`) and `origin/dev/Francisco` (693b05d) for string overlap. Gate 1 approved Q1-Q8 as the planner recommended.
Verification: `./gradlew assembleDebug lint --rerun-tasks` gives BUILD SUCCESSFUL, all 46 tasks executed. Lint reports 0 errors and 13 warnings, the baseline in `state.md`. None is in `MainTopAppBar.kt` or `values-pt/`, and no `top_bar_*` or `cd_*` string is unused or untranslated. `debugRuntimeClasspath` resolves `material-icons-core` to 1.7.8, `androidx.compose.ui:ui` to 1.10.4 and `kotlinx-serialization-core` to 1.9.0, as `state.md` says. There are no tests, by project decision, and this review does not ask for any.
Not verified: the previews were not rendered, because that needs Android Studio. The 48dp targets come from `IconButton`'s documented behaviour and were not measured. The bar has not run on a device, because nothing calls it until D-006.

## Verdict
**Approve after S1.** Nothing blocks. The code matches the coding plan's Interfaces section, apart from the two formatting changes recorded in `state.md`. One fix is needed: the Portuguese strings use "tickets" where the brief and Francisco's strings use "ocorrências". It is a small change to `values-pt/strings.xml` only. The D-005 preview pane check is still Diogo's to do before the task closes.

## What was checked and holds
- **Route rules.** The back arrow depends only on `canNavigateBack`, on any screen. The toggle shows only on `LIST`. The add button shows on every screen except `REPORT`. Titles come from an exhaustive `when` with no `else`. All of this matches the 00-plan table.
- **Stateless, MVVM.** The bar has no `remember`, no ViewModel and no side effects: values come in and clicks go out. `modifier` is the first optional parameter and is applied to the root `TopAppBar`.
- **Imports.** Only Compose, Material3, icons, `R` and `CityFixTheme`. Nothing from `androidx.navigation`, `viewmodel` or `model`.
- **Accessibility.** All four icons take their `contentDescription` from `stringResource`. The toggle's description names the next action and changes with `isExpanded`, as does its icon. Every control is an `IconButton`. The back arrow is `AutoMirrored`.
- **i18n.** No hard-coded user-visible text. The 9 strings in `values/` all exist in `values-pt/`, including `app_name`, and lint reports no `MissingTranslation`. The `PORTRAIT`, `LANDSCAPE` and preview `name` values are tooling text, not UI text.
- **Orientation.** Five preview functions, each with portrait and landscape, cover all four screens and both list states: 10 previews. The title has `maxLines = 1` with an ellipsis, and the bar keeps no state that rotation could lose.
- **Dependency.** Added through the catalog with no version, managed by the BOM. No other catalog entry changed, and `MainActivity.kt` and `Destinations.kt` are untouched.
- **Anonymous rule, permissions.** The bar receives no ticket or user data and requests no permissions. Hiding the add button by role is deferred to the Auth phase, as decided at Gate 1 (see N3).
- **Commits.** Conventional, with the task ID and no attribution. `.idea/` is not staged.
- **Can it be explained at the defence?** Yes. It is one enum and one composable with one `when` and three `if`s. The only things to prepare are `@OptIn` (`TopAppBar` is experimental) and the multi-preview annotation class.

## Blocking
None.

## Should-fix
- **S1. The Portuguese strings do not use the brief's or Francisco's term.** The brief says "ocorrências" to users (listing, detail, management) and keeps "ticket" mostly for the lifecycle. Francisco's `values-pt` already says "Fotografia da ocorrência" and "Não há ocorrências ativas para mostrar". After the merge, a Portuguese user would see "Tickets" as the title and "ocorrências" in the same list's empty state. "Reportar problema" is a third wording for the same action. Fix, in `values-pt/strings.xml` only:

  ```xml
  <string name="top_bar_title_list">Ocorrências</string>
  <string name="top_bar_title_detail">Detalhes da ocorrência</string>
  <string name="top_bar_title_report">Reportar ocorrência</string>
  <string name="cd_expand_tickets">Expandir ocorrências</string>
  <string name="cd_collapse_tickets">Recolher ocorrências</string>
  <string name="cd_report_ticket">Reportar ocorrência</string>
  ```

  These replace strings fixed in the coding plan, so record the change as a deviation in `state.md`. English stays as it is.

## Nit
- **N1. "Keep both sides" is wrong for `app_name` at the merge.** `origin/dev/Francisco` declares `app_name` with `translatable="false"` in `values/strings.xml` and leaves it out of `values-pt`. Keeping D-005's `app_name` in `values-pt` next to that marks a non-translatable string as translated, which lint flags (not run here, since the merge has not happened). Fix: at the merge, keep Francisco's `translatable="false"` line, delete `app_name` from `values-pt/strings.xml`, and run lint. Add this to the note for Francisco in 03-docs-plan. Until then the current file is correct: without `translatable="false"`, lint needs the translation.
- **N2. Only the title is exhaustive, not the control rule.** 00-plan says the "which control on which screen" rule sits in a `when`, so the compiler forces a decision for a new screen. The code uses `if`s, as the coding plan shows, so a new screen fails to compile for its title only and silently gets the add button. The code is fine; the claim is too broad. Fix: in the Docs phase, say only what 03-docs-plan already says (a new screen is a compile error until it has a title), and do not repeat the 00-plan wording. Be ready to say this at the defence.
- **N3. The role check is written down as optional.** `state.md` calls hiding the add button by role a requirement for the Auth phase. 03-docs-plan lists it under "Improvement opportunities", and no task file covers it. Fix: in the Docs phase, put it in overview.md's "Top bar (D-005)" subsection as a requirement, and carry it into the Auth task when that is written.
- **N4. The D-006 contract has one trap.** `navController.previousBackStackEntry` is not Compose state, so reading it alone does not cause a recomposition, and the arrow can go stale. Fix: in the Docs phase, add to the contract that D-006 reads it in the same scope as `currentBackStackEntryAsState()`, which it needs for `hasRoute` anyway.
- **N5. The preview check is still open.** Before Docs, Diogo runs the slice 3 check in the preview pane, confirms the private `OrientationPreviews` annotation produces both variants, and records the result in `state.md`. When D-006 runs the bar, also check landscape on an emulator with a display cutout: no controls should sit under it.
