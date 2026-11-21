## Files
| Path | Change | Why |
| --- | --- | --- |
| `ui/components/AttachmentViewer.kt` | `decorFitsSystemWindows = false` | Bug 2 (edge to edge) |
| `ui/screens/ShowScreen.kt` | open attachment state hoisted from `MediaSection` | Bug 2 (real cause, added after re-test) |
| `MainActivity.kt` (`openTab`) | drop `saveState`/`restoreState` | Bug 4; superseded by D-020 at the merge |
| `ui/screens/PendingCitizensScreen.kt` | `AsyncImage` thumbnail, icon fallback | Bug 6 |
| `res/values/themes.xml`, `res/values-night/themes.xml` | `forceDarkAllowed=false` | Bug 7 (replaces the snackbar colour idea) |
| ~~`data/FirebaseAuthRepository.kt`, `viewmodel/LoginViewModel.kt`~~ | ~~one-time unenrol~~ | Bug 1: dropped, Q1 = recreate the account |

## Steps
One slice per bug (see 00-plan Slices). Each: change, `assembleDebug` + `lint`, check on the emulator, commit.

## Conventions
kotlin-conventions; comments say why; strings in both `strings.xml` if any are added.

## Do not touch
Firestore/Storage rules, `google-services.json`, Diogo's files beyond these lines.
