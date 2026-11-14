# F-013: Image compression, thumbnails and WorkManager uploads
Branch: dev/Francisco · Base: cc36556
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-14 | feature, task file `docs/tasks/phase-2/F-013-media-upload-workmanager.md` |
| Plan | done | 2026-11-14 | 00-plan, 01-coding-plan (6 slices, D1-D12), 03-docs-plan; Q1-Q12 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-14 | approved; Q1 and Q7 class approach, slice 5 dropped, rest defaults |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-14: fetched origin; origin/main had 0 commits missing from dev/Francisco; the person chose not to merge (already up to date). Started F-013 from the task file. dev/Francisco equals origin/main plus the F-012 merge record (F-012 in origin/main at 0c17cb0).
- 2026-11-14 (Plan): planner read the task file, the brief (IV, V, I.1.B), decisions 0006, 0010, 0011, 0013, 0014, the overview handovers (F-010, F-011, F-012), the repository, mapping, attachment UI and container code, `storage.rules` / `firestore.rules`, and the class material:
  - slides 08: WorkManager `CoroutineWorker`, `Result.retry()`, `Constraints.Builder()`, `getInstance(context).enqueue`;
  - 09_Storage: Firebase Storage named only;
  - P09: `coil-network-okhttp` 3.0.4;
  - P10 Challenge I: `BitmapFactory.decodeFile`, `createScaledBitmap`, `(applicationContext as ContactsApp)`;
  - T8 AMOVStorageDemo: `bitmap.compress(JPEG, 90, stream)`, Storage dependency never called;
  - Contacts_#3: local paths + Coil.
  - Wrote `00-plan.md`, `01-coding-plan.md`, `03-docs-plan.md`. Approach: compress and make thumbnails at submit (`AttachmentUploader.prepare`, UUID names), then one `UploadWorker` per attachment (CONNECTED, exponential backoff 30 s). The worker uploads to the 0010 Storage paths and updates the entry with a Firestore transaction (download URLs, UPLOADED; FAILED after 5 attempts); `arrayUnion` on `publicTickets.thumbnails` only for the creator's REPORT media of PUBLIC tickets. A joiner's media go into `reports/{uid}`. The detail tiles show pending/failed. 6 slices.
  - Versions checked 2026-11-14: `androidx.work` 2.12.0 is the newest stable on Google Maven (the class names no version).
  - Open questions Q1-Q12 with defaults: transaction; download URLs; delete local copies; video remux slice; drop `removeImageLocation`; no UPLOADING; plain enqueue; indicator in `MediaTile` instead of `AttachmentGallery`; no video size check; `-ktx` artifact; leave rule drafts unchanged (the `reports/{uid}` update gap); the person confirms the Storage bucket is in test mode.
  - Departures from the task file, pending Gate 1: indicator location (Q8), plain `enqueue` instead of unique work (Q7), no UPLOADING (Q6).
- 2026-11-14: Gate 1: approved by the person with these answers. Q1: **follow the class approach**: class teaches plain `get()` then `set`/`update` with listeners (T8 demo), no transactions, so the worker reads the ticket (or report) document, replaces its attachment entry in the list and writes the list back with `update` (no `runTransaction`). Accepted risk: if two workers of the same ticket finish at the same moment, one write can overwrite the other's entry (it then stays PENDING locally until a later write); recorded as a known limit. Q2 default (https download URLs in `storagePath`/`thumbnailPath`, decision 0010 amended). Q3 delete local copies once UPLOADED. Q4 slice 5 (video location) **dropped**: the brief does not require removing file metadata (I.1.C hides georeferencing on screen); photos lose all EXIF anyway when re-encoded for compression; videos are uploaded as recorded and may carry their recording location (known limit, improvement opportunity). Q5 default (drop `removeImageLocation`, re-encoding removes EXIF). Q6 default (no UPLOADING). Q7 **class approach**: plain `WorkManager.getInstance(context).enqueue(request)` as on slides 08. Q8 default (indicator in the detail tiles). Q9 default (no video size check). Q10 default (`work-runtime-ktx` 2.12.0). Q11 default (draft rules left alone; gap recorded). Q12: explained to the person (check that the default Storage bucket exists and its rules are in test mode); needed before the device checks, not before coding. Decision number 0015 confirmed free on origin/dev/Diogo.
