# F-006: ReportScreen — review
Reviewed: 2026-11-01 · Range: `763d787..HEAD` (a04e88c, 7dd4b0b, 1a28697, d76eec5, plus `state.md` in cdf0df4)
Against: 00-plan and 01-coding-plan (revision 3, Gate 1 approved with all defaults), the deviations in `state.md`, the task file `docs/tasks/phase-1/F-006-report-screen.md`, the brief (extracted with `pdftotext -layout`: I.2.E Abertura, I.3.E, V), `docs/architecture/overview.md`, `kotlin-conventions`, `.claude/rules/kotlin.md`, and class sheets P08 (UiState + `isValid`, route-scoped ViewModel, `isError` + `supportingText`, `imePadding` + `adjustResize`, `ExposedDropdownMenuBox`) and P09 (attachments, deferred to F-007 by the person's decision).

**Verdict: approve with should-fixes.** Nothing blocks. The code follows the approved plan and the Gate 1 defaults. MVVM is clean: `ReportUiState` and `ReportViewModel` hold no Compose, no Context and no text, `ReportContent` takes no ViewModel, and build and lint are at baseline. The three logged deviations are sound (see "Deviations"). The should-fixes are small. S1 settles the implementer's open point: the no-categories message should win. S2 makes `isValid` the one gate for `submit()`, so F-007 cannot add the attachment rule in one place and miss the other.

## What was checked
- **Build and lint (run by the reviewer):** `./gradlew assembleDebug` passes. `./gradlew lint` gives 0 errors and 13 warnings, the baseline: AndroidGradlePluginVersion 1, GradleDependency 1, NewerVersionAvailable 3, RedundantLabel 1, UnusedResources 7 (all `colors.xml`: purple_200/500/700, teal_200/700, black, white). No UnusedResources or MissingTranslation on a new key, and no deprecation from the dropdown API.
- **Base:** `state.md` says `Base: f2714b5`. `f2714b5..763d787` holds only the start and plan commits, so the code range is the same.
- **Files touched:** `data/SampleCategories.kt`, `viewmodel/ReportUiState.kt`, `viewmodel/ReportViewModel.kt`, `ui/screens/ReportScreen.kt` (all new), both `strings.xml`, and `state.md`. `git diff --stat f2714b5..HEAD` over `model/`, `ui/navigation/`, `ui/components/`, `ui/theme/`, `TicketsViewModel.kt`, `SampleTickets.kt`, the manifest, the Gradle files and `docs/tasks/` is empty. The do-not-touch list holds.
- **Strings:** 16 keys in each file, same names, in a new `<!-- Report form -->` block. Before F-006 no key started with `report_` or `scope_`, so nothing collides with Diogo's `top_bar_` / `cd_` (his `cd_report_ticket` is a different key). PT uses "ocorrência" ("Descreva a ocorrência.", "Não foi possível enviar a ocorrência.", "Enviar ocorrência") and pt-PT wording ("fotografias"). Reused keys `situation_*` and `ticket_media_title` exist in both files. Category names ("Pavement") are municipality data and are not translated (plan, Approach).
- **Brief:**
  - I.2.E: category, situation (`Situation.entries`: Dano/Avaria/Destruição/Roubo), description and scope (`Scope.entries`) are all required, with nothing preselected (Q9). The "at least one image/video" rule is the accepted gap that F-007 closes (Q2). The `ReportUiState` KDoc says where it goes. See S2 for the second place it must go.
  - I.3.E: `ReportViewModel` puts only `SampleCategories.all.filter { it.active }` in the state, so "Graffiti" never appears. `submit()` also rejects an id that is not in `categories`.
  - V: Compose only, EN default + PT, portrait and landscape (two panes, hoisted scroll states), accessibility, and the stateless/stateful split. No runtime permission is needed in this task.
- **ViewModel logic** (`ReportViewModel.kt`):
  - Submit lock: `submit()` returns `null` while `submitted`. A valid submit sets `submitted = true` before returning. `onSubmitResult(false)` unlocks and sets `submitError`, and `onSubmitResult(true)` keeps the lock. A double tap cannot create two tickets, because `onSubmit` runs synchronously inside the first click.
  - Error timing: the description error is recomputed on every change, and each choice clears its own error and `submitError`. Submit sets the error of every missing field. On a fresh form `submit()` sets four errors and `showErrorSummary`, and returns `null`, as the plan requires.
  - `showErrorSummary` is recomputed after every event as `showErrorSummary && !isValid` (`withSummaryRefreshed`), so it goes away once the form is valid and never turns on before a Submit.
  - The description is trimmed in `ReportSubmission`, and a whitespace-only one is invalid. No `!!`: `situation`/`scope` are read into locals and smart-cast after the combined `if`.
- **Compose:**
  - `ReportScreen` is the only place that touches the ViewModel. `ReportContent` holds only UI element state: two hoisted `rememberScrollState()`s, `LocalConfiguration` and, inside `CategoryDropdown`, `expanded`.
  - Every composable has `modifier: Modifier = Modifier` as its first optional parameter and applies it once, to its root. `ReportContent` applies it to the root of whichever branch is drawn.
  - No hard-coded user-visible text.
- **Accessibility:**
  - Situation and Visibility titles and the media title have `heading()`.
  - Radio rows are `selectable(role = Role.RadioButton)` inside `selectableGroup()`, at least 48dp high and full width, with `RadioButton(onClick = null)`, so each row is one merged node.
  - The description and category fields have `error` semantics with the real message.
  - The summary and the submit error are polite live regions.
  - Errors are always shown as text, never by colour alone. See S1, N1 and N2.
- **Privacy:** the form shows no ticket, author or coordinate data, and nothing is preselected. In particular PUBLIC is never chosen for the user. Anonymous users can reach the form in phase 1; hiding it is the Auth task's job (overview), as the plan says.
- **Previews:** empty, filled and error each have EN, PT, EN landscape and PT landscape. No categories has EN and PT, and submit failed has EN. All are `private`, wrapped in `CityFixTheme`, and built from `ReportUiState` values with no ViewModel. `previewCategories` copies the three active samples on purpose, so that `ReportScreen.kt` imports nothing from `data/`.

## Deviations (logged by the implementer)
- **`imePadding()` before `verticalScroll` (`ReportScreen.kt:119-120, 130-131, 138-139`): agreed, and the plan was wrong.** Padding placed outside the scroll container shrinks the viewport, so the focused field's bring-into-view request scrolls it above the keyboard. Placed inside, as the plan had it, it only adds blank space after the last item, and the description can stay under the keyboard. One caveat, already in the D-006 handover: until D-006 passes `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)`, the navigation-bar inset is counted twice while the keyboard is open. `MainActivity` today uses `padding(innerPadding)` only, so the handover matters.
- **`menuAnchor(PrimaryNotEditable, enabled = hasCategories)` (`ReportScreen.kt:258`): agreed.** It uses the non-deprecated 1.4.0 overload, and TalkBack is not offered an empty menu.
- **`error` semantics on the category field (`ReportScreen.kt:260`): agreed**, with the change in S1 for the no-categories case.

## Blocking
None.

## Should-fix

**S1. With no categories, the "no categories" message should win (the implementer's open point).** `ReportScreen.kt:240-241`:
```kotlin
val supportingRes = error ?: if (hasCategories) null else R.string.report_no_categories
```
After a Submit with an empty list, the field says "Choose a category." under a field that cannot be opened. That message asks for something the user cannot do. The reason the form cannot be sent is that there are no categories (I.3.E). It gets worse: the field is `enabled = false`, and M3 `OutlinedTextField` checks `enabled` before `isError`. So the error text is drawn in the disabled supporting colour (38% alpha), not in red, and the outline is not red either. The message is faint and fails contrast. The same faint colour applies to "No categories are available." before any Submit. **Verdict on the open point: yes, the no-categories message should win.**
- Fix, in `CategoryDropdown`:
  ```kotlin
  val supportingRes = if (hasCategories) error else R.string.report_no_categories
  val supportingText = supportingRes?.let { stringResource(it) }
  ```
  - Use `supportingText` both for `supportingText = supportingText?.let { { Text(it) } }` and for the semantics: `.semantics { if (error != null && supportingText != null) error(supportingText) }`. TalkBack then says "No categories are available." after a Submit.
  - Make the message readable on the disabled field: `colors = OutlinedTextFieldDefaults.colors(disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant)`.
  - Add a preview of that state, EN and PT: `noCategoriesState.copy(categoryError = R.string.report_error_category, showErrorSummary = true)`.
  - Record the decision in `state.md`. No ViewModel change is needed.

**S2. `submit()` does not use `isValid`, so the validation rules live in two places.** `ReportUiState.isValid` (`ReportUiState.kt:34-38`) and `submit()`'s own checks (`ReportViewModel.kt:69-75`) list the same rules separately. The `ReportUiState` KDoc tells F-007 to add `attachments.isNotEmpty() && attachmentsError == null` to `isValid`, but `submit()` never reads `isValid`. If F-007 follows the KDoc, a report without an image/video would still pass `submit()`: the I.2.E gap would stay open while looking closed. It also departs from the P08 pattern the plan cites (slide 15: `if (!isValid) return`), and at the defence "which of the two decides?" is hard to answer.
- Fix: build the state with the errors first, then gate on `isValid`, keeping the locals for the smart cast:
  ```kotlin
  val checked = state.copy(
      categoryError = if (categoryMissing) R.string.report_error_category else null,
      situationError = if (situation == null) R.string.report_error_situation else null,
      descriptionError = if (descriptionMissing) R.string.report_error_description else null,
      scopeError = if (scope == null) R.string.report_error_scope else null,
  )
  if (!checked.isValid || situation == null || scope == null) {
      _uiState.value = checked.copy(showErrorSummary = true)
      return null
  }
  ```
  Then extend the `ReportUiState` KDoc: "F-007 also sets `attachmentsError` in `ReportViewModel.submit()`". Re-run build and lint.

## Nit

**N1. `error` semantics on the radio-group `Column` are never read.** `ReportScreen.kt:292-296` puts `error(errorText)` on the `selectableGroup()` column. That column is not focusable and does not merge its children, so TalkBack never lands on it and never speaks that property. The error is read only as the visible `Text` after the last option (`ReportScreen.kt:323-329`). This works, but it differs from what `state.md` and acceptance check 9 claim.
- Fix: remove `.semantics { if (errorText != null) error(errorText) }` from the group and rely on the visible error text, or keep it with a comment that says it is informational only. Either way, word the docs phase accordingly.

**N2. The error summary may not be announced again on a second failed Submit.** The polite live region (`ReportScreen.kt:384`) announces a change in text. If the summary is already visible, a second failed tap changes nothing, and a TalkBack user hears nothing. Whether Compose announces a live-region node when it first appears also needs a device check.
- Fix: check with TalkBack after D-006. If the summary is silent, move focus to the first field with an error after a failed Submit (a `FocusRequester` per field, requested from `ReportScreen` when `submit()` returns `null`), which also helps keyboard users. Also check that the description is not read twice (supporting text plus `error` semantics).

**N3. `expanded` is not actually restored across rotation.** `rememberSaveable` keys its value by position in the composition. `CategoryDropdown` sits under a portrait `Column` in one branch and a landscape `Row` in the other, so after a rotation the menu starts closed. That behaviour is fine, and arguably better, but the plan's "Failure modes: dropdown `expanded` is saveable" suggests otherwise.
- Fix: no code change. In the docs phase, say that the menu closes on rotation and that `rememberSaveable` only keeps it open across other configuration changes. Alternatively use `remember` and drop the claim.

**N4. Rotation keeps only the left scroll position.** Portrait drives `leftScroll`, so a user scrolled down to the description in portrait finds the right pane at the top in landscape, with the left pane clamped. Nothing is lost, and the hoisting is as planned (F-005 S2).
- Fix: no code change. Mention it in the docs as the expected behaviour.

**N5. The submit wiring is a dense one-liner.** `ReportScreen.kt:83`, `{ viewModel.submit()?.let { viewModel.onSubmitResult(onSubmit(it)) } }`, packs three steps into one expression, and both authors must be able to explain it.
- Fix:
  ```kotlin
  onSubmitClick = {
      val submission = viewModel.submit()          // null: errors shown, or already submitted
      if (submission != null) {
          val created = onSubmit(submission)       // D-006 calls reportTicket
          viewModel.onSubmitResult(created)
      }
  },
  ```

**N6. The PT summary wording.** "Faltam preencher campos obrigatórios." is colloquial.
- Fix: `report_error_summary` = "Há campos obrigatórios por preencher." in `values-pt/strings.xml`.

**N7. A submit-failed preview in PT is missing.**
- Fix: add `@Preview(showBackground = true, locale = "pt")` to `ReportSubmitFailedPreview`, so "Não foi possível enviar a ocorrência." is checked in place.

**N8. `scopeLabelRes` has no `@StringRes` on its return type** (`ReportScreen.kt:59`).
- Fix: add `@StringRes` to `scopeLabelRes`. If D-006 or a later task needs it outside this screen, move it next to `situationLabelRes` in `ui/components/` at that point.

## Manual checks left to the person
- Android Studio previews: field order, nothing selected, EN and PT error texts, two panes in landscape with Submit reachable in the right pane, and the S1 state once added.
- After D-006 wires the route, on an emulator:
  - rotation keeps values and errors;
  - leaving and reopening starts empty;
  - the keyboard does not cover the description in either orientation;
  - no double navigation-bar inset with `consumeWindowInsets(innerPadding)`.
- TalkBack: one node per radio row with its state, headings, the dropdown read as a labelled field with its error, and the live-region behaviour in N2.
