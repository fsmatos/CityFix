# F-021 review

Scope: 619fee6, fbf7d25, 90d04c3, 2159a04 and the merge d300eb9 (`ShowScreen.kt` and `MainActivity.kt` resolution), as they are at HEAD (816b13b). Checked against 00/01 plans, the state.md Log, `.claude/rules/kotlin.md` and kotlin-conventions.

Summary: the code is small, correct and matches the plan and the recorded changes. No blocking issues. 7cc2eb8 is fully gone: `openTab` in `MainActivity.kt:474-487` is D-020's version (`saveState`/`restoreState` kept, reselect handling), and no F-021 comment or half change is left in `MainActivity.kt`. The merged `ShowScreen` passes both `onOpenAttachment` and D-019's `responsibleNames` in both orientations (`ShowScreen.kt:155-205`). Previews still compile (author's build and lint). No strings added, so `strings.xml` does not change. Nothing new reaches anonymous users: the viewer and tiles are as before, and the pending list is operator only.

## Blocking
None.

## Should-fix

1. `app/src/main/java/pt/isec/amov/cityfix/ui/components/AttachmentViewer.kt:62-63`: the comment gives the wrong cause, and that is hard to explain at the defence. It says the dialog "was inset by the system bars after a rotation", but the state.md Log (bug 2 re-opened) found the real cause: the viewer closed because `openedId` lost its saved state, which fbf7d25 fixed. 619fee6 was kept only so the viewer is edge to edge (most visible in landscape).
   Fix: reword to the reason it was kept, e.g. `// decorFitsSystemWindows = false: the black viewer also draws under the system bars (edge to edge), most visible in landscape (F-021); the close button keeps safeDrawingPadding.`

2. `docs/work/F-021-testing-bug-batch/01-coding-plan.md` Files table and the `state.md` Code row are out of date after the deviations. The Files table still lists `MainActivity.kt` "drop saveState/restoreState" (superseded by D-020 at the merge) and "SnackbarHost or Theme.kt" for bug 7. It does not list `ShowScreen.kt` (fbf7d25) or `values/themes.xml` + `values-night/themes.xml` (2159a04). The state.md Code row still names 7cc2eb8 as the bug 4 fix.
   Fix: in the docs phase, add a Files table to `state.md` (or a note in the Log) with the final list: `AttachmentViewer.kt`, `ShowScreen.kt`, `PendingCitizensScreen.kt`, both `themes.xml`, and `MainActivity.kt` "no F-021 change after the merge (D-020 kept)". Update the Code row so it does not point at 7cc2eb8 as the live fix.

## Nit

1. `app/src/main/java/pt/isec/amov/cityfix/ui/screens/PendingCitizensScreen.kt` (`PendingCitizenCard`, the `if (photo != null)` branch): a blank `photo` string or a failed load (offline, deleted file) leaves an empty grey box instead of the person icon the comment promises.
   Fix: use `if (!photo.isNullOrBlank())`. For a failed load, either add `error = rememberVectorPainter(Icons.Outlined.Person)` to `AsyncImage` or reword the comment to "icon when there is no photo". One line either way; skip if not worth it.

2. `PendingCitizensScreen.kt` previews: the sample citizens (lines 145-146) have no `photo`, so only the icon branch is previewed. Optional: keep as is (a preview cannot load a URL anyway), or say in the comment that the photo branch needs a device.

3. `ShowScreen.kt:130-133`: the F-021 comment is good. To make the defence easier, it could also say that `MediaSection` now only reports taps (`onOpen`), so it is a stateless component (state hoisting, as in class). Wording only.
