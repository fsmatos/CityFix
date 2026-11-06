# F-007: Report attachments — review
Reviewed: 2026-11-06 · Range: `1487976..HEAD` (`state.md` Base), which is 8248601, 10224ae, f40574a, cccf188, 18f0c7b, 9a1f442, 044600d, be0dff1, ab35051
Against: 00-plan and 01-coding-plan (revision 3, Gate 1 approved), 03-docs-plan, the deviations in `state.md`, the task file `docs/tasks/phase-1/F-007-report-attachments.md`, the brief (I.2.E Abertura, extracted with `pdftotext -layout`), `docs/architecture/overview.md`, `kotlin-conventions`, `.claude/rules/kotlin.md`, and the P09 text (all 28 slides).

**Verdict: approve with should-fixes, and F-007 is not done until the device checks run.** Nothing blocks at code level. The code follows the approved plan, MVVM is clean, and build and lint are at the expected count. S1 is the one real correctness gap: the camera path does not check the limit, so a fast tap on both add buttons can reach 4 attachments. S2 loses a photo's file on a double tap. S3 is the stale KDoc. **Nothing in this review ran on a device or emulator**, so no behaviour below is proven at runtime (see "Acceptance checks").

Range note: the request said `8248601..HEAD`, but that excludes 8248601 itself (Coil, `<provider>`, `file_paths.xml`). I reviewed from the Base, so that commit is covered.

Not reported, as decided at Gate 1: the main-thread copy (Q13), attachments lost on process death (Q3), Q2, Q6-Q12, and the absence of tests.

## What was checked
- **Build and lint (run by the reviewer, forced with `--rerun-tasks`, all 46 tasks executed):** `./gradlew assembleDebug` and `./gradlew lint` pass. Lint gives 0 errors and 16 warnings: AndroidGradlePluginVersion 1, GradleDependency 2, NewerVersionAvailable 5 (two are the accepted Coil 3.0.4 vs 3.6.3), RedundantLabel 1, UnusedResources 7. That is the baseline 14 plus the two accepted Coil ones. No new UnusedResources or MissingTranslation. The compiler prints 7 annotation-target warnings on `ReportUiState.kt`; see N6.
- **Files touched:** `git diff --stat 1487976..HEAD` over `model/`, `data/`, `ui/navigation/`, `ShowScreen.kt`, `TicketCard.kt`, `ListScreen.kt`, `DashboardScreen.kt`, `backup_rules.xml`, `data_extraction_rules.xml` and `docs/decisions/` is empty. The do-not-touch list holds. `MainActivity.kt` gains exactly the one planned line.
- **Dependencies:** resolved `debugRuntimeClasspath` confirms `runtime` and `ui` 1.11.0, `foundation` 1.10.4, `profileinstaller` 1.4.1, `activity` 1.13.0.
- **Coil 3.0.4 on Compose `ui` 1.11 (static only):** I parsed the class files of every Coil 3.0.4 artifact and looked up each of their 389 `androidx.compose.*` member references in the resolved Compose versions. No class is missing. The only 6 misses are `hashCode()` inherited from `Object`, so false positives. This rules out a `NoSuchMethodError` on those references. It does not prove the images draw, so the state.md risk stays open until a device run.
- **Manifest and permissions:** the merged manifest has no `CAMERA` and no `INTERNET`; its only `uses-permission` is androidx's `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. No runtime permission is needed. `launchCamera` catches `ActivityNotFoundException` and `SecurityException` as planned.
- **FileProvider:** authority `${applicationId}.fileprovider` in the manifest matches `"${context.packageName}.fileprovider"` in `FileUtils` (no `applicationIdSuffix`). `<files-path path="images/">` matches `File(context.filesDir, "images")`, so slide 24's "Failed to find configured root" does not apply. `exported="false"`, `grantUriPermissions="true"`, as slide 9.
- **Limit across picks:** `attachPicked` reads `viewModel.uiState.value` at callback time, not the composition's `state`, so it is fresh after a rotation. At 0 remaining, `take(0)` copies nothing and shows the notice. Over-limit files are never written. `PickMultipleVisualMedia()` with `maxItems = remaining` does work: the activity 1.13.0 source uses `min(maxItems, input.maxItems)`. `remaining > 1` keeps its `require(> 1)` satisfied. The camera path is S1.
- **File lifecycle:**
  - Success: `submitted` is set before `onSubmit`, and `navigateUp` runs inside `onSubmit`, so `onCleared` always sees the lock and keeps the files.
  - Failed submit: `onSubmitResult(false)` unlocks, the files stay for a retry, and leaving deletes them.
  - Remove deletes the file. Leaving without submitting deletes all of them.
  - Cancel: an empty pick returns before any work, and a cancelled capture deletes its file. `createTempImageFile` does not create the file; the camera does, so the delete is a no-op when nothing was written.
  - Rotation: `viewModelStore` survives, so `onCleared` does not run.
  - Pending camera file: `onCleared` does not know it, which is fine because the form cannot be left while the camera is open. The only leak left is process death with no return (Q10), plus S2.
- **`rememberSaveable` and launchers:** `tempCameraPath` and all three launchers are in `ReportScreen`, above the orientation branch in `ReportContent`, so their positions match after a rotation. Comment present and correct. The callback reads the path into a local and clears it. No `!!`.
- **`isVideoPath` and `VID_`:** names are always generated (`IMG_`/`VID_` + UUID, `CAPTURE_` + millis). The source's display name is never used, so nothing can spoof a prefix or traverse a path. `CAPTURE_` correctly falls to IMAGE. Unknown MIME: N3.
- **Grid in the scrolling form:** `heightIn(max = 240.dp)` stays in `AttachmentGallery`, so the grid's maximum height is finite inside `verticalScroll`; the KDoc says why it must stay. By calculation on 360x640 landscape: pane 292dp, 2 columns, 142dp tiles, 2 rows = 292dp, over the 240dp cap. The grid then scrolls on its own, and about 90dp of the third tile shows without scrolling, enough for its 48dp remove button. Previews were not rendered.
- **MVVM:** `ReportUiState` has no Compose, Context or text. `ReportViewModel` is no-arg with no `Context` and no coroutines. `ReportContent` takes no ViewModel. File work happens only in click handlers, launcher callbacks and ViewModel events, never in composition. `isValid` gains the term and `submit()` sets the error, so F-006 S2's single gate holds.
- **Q13 comment:** present above `attachPicked` (`ReportScreen.kt:148`), citing slide 26 and 00-plan Q13. Requirement met.
- **Strings:** EN 73 keys, PT 72; the only difference is `app_name` (`translatable="false"`). All 11 planned keys match the plan's text in both files, PT is pt-PT ("fotografia", "câmara", "ficheiros"), and `report_media_pending` and `MediaPendingSection` are gone.
- **Accessibility (by reading):** both add buttons have visible text. Tiles have "Photo N"/"Video N", and remove buttons "Remove photo N"/"Remove video N", with type and position, so TalkBack can tell them apart. The play badge is decorative (`null`), since the tile already says "Video". The remove `IconButton` is 48dp. The heading has `heading()`, and errors and notices are polite live regions shown as text.
- **Privacy:** no regression for anonymous users.
  - `reportTicket` sets `uploadedById = LOCAL_USER_ID`, and `toAnonymousView()` blanks it.
  - PRIVATE tickets stay out of `tickets`.
  - `ShowScreen` reads only `attachment.type`, and `TicketCard` reads no attachment.
  - `Attachment.file` (an app-private path with a UUID, no personal data) and `id` pass through the projection as decision 0006 expects, but nothing draws them.
  - **Handover:** a camera photo can carry GPS in its EXIF, which is exact coordinates. Whoever replaces `MediaTile` with Coil must strip EXIF and video metadata first, not only at upload, or the anonymous detail would hold them. D-007 or the media task should say so.

## Deviations (logged by the implementer)
- **(1) `tools:ignore="PluralsCandidate"`: agreed.** The value is the constant 3, so the singular never shows.
- **(2) `onPhotosAttached` returns early on an empty list: agreed, the plan was wrong.** Clearing the error with nothing attached would hide the required error.
- **(3) `onRemoveAttachment` clears the notice and `submitError`: agreed.** A limit notice should not stay once a slot frees up. See N1 for the other paths.
- **(4) Strings land with their first use, (5) preview states updated: agreed.**
- **(6) "P09 is not in the repository":** now checked against the text. `FileUtils` follows slides 11-12, the provider slide 9, the grid slide 27, and the callback slide 26, with the differences 00-plan lists. The null-stream case is better than slide 11, which returns a path to a file that was never written.

## Blocking
None at code level. The runtime checks below are still required before F-007 can be closed.

## Should-fix

**S1. The camera path does not check the limit, so the count can reach 4.** `attachCaptured` (`ReportScreen.kt:172-180`) calls `onPhotosAttached(listOf(path))` with no check, and `onPhotosAttached` (`ReportViewModel.kt:66`) has none either. `isValid` has no upper bound. The only guard on the camera is the button's `enabled`, which is read at tap time.

Path, from reading the code: with 2 attachments, tap "Take a photo" and then "Add from gallery" before the first activity covers the form. `canAddAttachment` is still true for both taps. The picker is on top, returns first and takes the last slot, giving 3. The camera then returns and adds a fourth. That breaks the acceptance check "the limit of 3 holds across picks and the camera", and `submit()` accepts it, so the ticket gets 4 attachments.

`remainingAttachments` (`ReportUiState.kt:40`) then goes negative. `canAddAttachment` handles that, but `uris.take(-1)` in `attachPicked` throws `IllegalArgumentException`, so a further pick result in flight would crash the app. 00-plan calls the `take` "the real rule", but it covers only one of the two ways in.
- Fix, keeping the plan's shape:
  ```kotlin
  // ReportUiState
  val remainingAttachments: Int
      get() = (MAX_REPORT_ATTACHMENTS - attachments.size).coerceAtLeast(0)

  // ReportScreen
  private fun attachCaptured(viewModel: ReportViewModel, path: String, success: Boolean) {
      val file = File(path)
      val hasPhoto = success && file.length() > 0
      when {
          hasPhoto && viewModel.uiState.value.remainingAttachments > 0 ->
              viewModel.onPhotosAttached(listOf(path))
          hasPhoto -> {
              file.delete()
              viewModel.onAttachmentLimitReached()
          }
          else -> {
              file.delete()
              if (success) viewModel.onCameraUnavailable()
          }
      }
  }
  ```
  Update the 00-plan wording in the docs phase: the limit is checked in both callbacks.

**S2. A double tap on "Take a photo" leaves an unreferenced file.** `onTakePhotoClick` (`ReportScreen.kt:133-141`) has no guard, and a Compose click has no debounce. The second tap runs before the camera covers the form, creates a second path and overwrites `tempCameraPath`, so the first path is lost. If the user captures in the first camera instance, that file has no owner: the callback does not know it, and `onCleared` only deletes listed attachments. It stays on disk with nothing referring to it, and the user's photo never appears. This is not the orphan Q10 accepted, which is about process death.
- Fix: ignore the tap while a capture is pending.
  ```kotlin
  onTakePhotoClick = {
      if (tempCameraPath == null) {
          val (file, uri) = FileUtils.createTempImageFile(context)
          tempCameraPath = file.absolutePath
          if (!launchCamera(takePhoto, uri)) {
              tempCameraPath = null
              file.delete()
              viewModel.onCameraUnavailable()
          }
      }
  },
  ```

**S3. The `reportTicket` KDoc is stale** (`TicketsViewModel.kt:45-48`). It lists only the blank strings as `null` cases, but the method now also returns `null` for empty attachments, and it does not mention that the attachments are stored. `state.md` records it as left as it was. Revision 3 of the coding plan has no line for it, and earlier revisions are not in git (only 8a1d1d8 touches that file), so I could not confirm that the original plan listed it. It is a defect either way, because this is the contract the caller reads.
- Fix (Diogo's file, so tell him; it is comment only):
  ```kotlin
  /**
   * Opens a new ticket from the report form (F-006, F-007). Returns its id, or null when
   * description, categoryId or categoryName is blank (all three are trimmed) or attachments is
   * empty. The attachments are stored as PENDING, uploaded by the local user. No location yet,
   * so no 10 m grouping (00-plan Q8).
   */
  ```
  The docs phase covers the same change in `overview.md`. `docs/tasks/phase-1/README.md:36` also needs the new signature, as 03-docs-plan says.

## Nit

**N1. A notice can outlive its cause.** `attachmentsNotice` clears only on a successful add or a remove. "A file could not be added" or "The camera could not be used." stays after a cancelled pick or capture, and through a failed Submit.
- Fix: add `fun onAttachmentNoticeDismissed()` that clears it, and call it at the start of `onAddFromGalleryClick` and `onTakePhotoClick`. `attachPicked` and `attachCaptured` set it again if needed.

**N2. "Null on any failure" is not literally true.** `getType` and `mediaFolder` run outside the `try` (`FileUtils.kt:26-28`). In practice `ContentResolver.getType` swallows its own failures, so this is about the contract, not a known crash.
- Fix: declare `var target: File? = null` before the `try`, do the two calls inside it, and use `target?.delete()` in the `catch`. Or soften the KDoc to "Null when the copy fails".

**N3. A video with an unknown MIME type is stored as an image.** `fileNameFor` treats anything not `video/*` as an image, as planned. A provider on the `ACTION_OPEN_DOCUMENT` fallback that returns `null` or `application/octet-stream` gives `IMG_....jpg` holding video bytes: the tile says "Photo N", shows the placeholder, and the ticket stores `IMAGE`. The photo picker returns real types, so this is limited to API 24-29 without the backport (Q9).
- Fix: no code change. Add it to the limits in the report notes.

**N4. `AttachmentTile` breaks a line of the overview.** "Shared components" says `ui/components/` has "no ViewModel, `remember` or Firebase", and no other component uses `remember`. `AttachmentGallery.kt:81` remembers the `ImageRequest` and reads `LocalContext`. That is sensible: a derived value, so Coil does not reload on each recomposition.
- Fix: no code change. In the docs phase, narrow the sentence to "no state; `remember` only for derived values such as the video `ImageRequest`".

**N5. Only the UI protects a submitted ticket's files.** `onRemoveAttachment` deletes the file with no `submitted` check, and `removeEnabled = !state.submitted` is the only guard. I found no reachable path, but `submitted` is what `onCleared` trusts.
- Fix: first line of `onRemoveAttachment`: `if (_uiState.value.submitted) return`.

**N6. Two more compiler warnings.** `attachmentsError` and `attachmentsNotice` repeat the existing `@StringRes` pattern, so `ReportUiState.kt` now gives 7 "annotation is currently applied to the value parameter only" warnings, 2 of them from F-007. They are not lint warnings, so `state.md`'s count is right.
- Fix: optional, and for all 7 at once: use `@param:StringRes`.

**N7. Two model KDocs now disagree with the data.** `Attachment.id` says "Firestore document id. Empty until persisted", and F-007 stores the file name. `Attachment.file` is already Diogo's under Q12.
- Fix: add `id` to the same handover in the docs phase. `model/` stays untouched here.

## Acceptance checks
Static means build, lint, manifest or source inspection. **Read only** means the code looks right but nothing ran. No device or emulator was used.

| Check (00-plan) | Status |
| --- | --- |
| Merged manifest has no `CAMERA`, no `INTERNET` | Verified, static |
| `ReportViewModel` no-arg, no `Context`; no file work in composition; no `!!` | Verified, static |
| `assembleDebug` passes, lint 0 errors, warnings 14 plus Coil | Verified, run by the reviewer on HEAD |
| Add up to 3 from gallery, take a photo, thumbnail, remove | **Unverified at runtime.** Read only |
| Submit with no attachment shows the error, no ticket | **Unverified at runtime.** Read only |
| Files copied to private storage, still shown after rotation | **Unverified at runtime.** Read only |
| Portrait and landscape, no clipped content; EN and PT | **Unverified at runtime.** Keys verified, previews not rendered |
| Labels and TalkBack | **Unverified at runtime.** Labels read only |
| Video shows a real frame with badge; bad file shows placeholder | **Unverified at runtime.** Never run: Coil and `VideoFrameDecoder` have not decoded anything yet |
| Limit of 3 across picks and camera, and on `ACTION_OPEN_DOCUMENT` | **Unverified at runtime; fails by reading** on the fast-tap path (S1) |
| Rotate with picker or camera open still adds the item (Test Case 3) | **Unverified at runtime.** Read only |
| Failed copy shows the notice and keeps the rest | **Unverified at runtime.** Read only |
| Cancel leaves no file | **Unverified at runtime.** Read only |
| Process death with camera open adds the photo | **Unverified at runtime.** Read only |
| Remove deletes; leaving deletes all; after submit files stay | **Unverified at runtime.** Read only; S2 is the exception |
| PUBLIC detail shows one tile per attachment | **Unverified at runtime.** Read only |
| Narrow landscape: third item and its remove reachable | **Unverified at runtime.** By calculation only |
| Coil 3.0.4 runs on Compose `ui` 1.11 (plan risk) | **Unverified at runtime.** All method references resolve; 3.4.0 is the fallback on a crash |

## Manual checks left to the person
Emulator setup: 01-coding-plan (slide 22). Run after S1 and S2 are fixed.
- First run: open the form, add a photo and a video. Any crash in `AsyncImage` means the Coil 3.4.0 fallback.
- Limit: fill 3 by picks, by camera and mixed; try a fast tap on both buttons at 2 attachments (S1); a fast double tap on "Take a photo" (S2).
- Rotation with the picker open and with the camera open; `adb shell am kill pt.isec.amov.cityfix` with the camera open.
- Device Explorer, `files/images`: after a cancel, a remove, leaving the form, a submit.
- Narrow landscape: third tile, its remove button, and dragging from the grid into the pane.
- EN and PT in both orientations. TalkBack: tile and remove labels, and whether the notice is announced on return from the picker or camera.
- A PUBLIC report's detail tiles; a PRIVATE report still confirms.
- API 24-29 AVD for the `ACTION_OPEN_DOCUMENT` fallback, or record it as not checked.
