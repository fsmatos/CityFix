## Files
| Path | Change | Why |
| --- | --- | --- |
| `ui/components/AttachmentViewer.kt` | `decorFitsSystemWindows = false` | Bug 2 |
| `MainActivity.kt` (`openTab`) | drop `saveState`/`restoreState` | Bug 4 |
| `ui/screens/PendingCitizensScreen.kt` | `AsyncImage` thumbnail, icon fallback | Bug 6 |
| `MainActivity.kt` (SnackbarHost) or `ui/theme/Theme.kt` | snackbar colours/position | Bug 7, after the dark-mode check |
| `data/FirebaseAuthRepository.kt`, `viewmodel/LoginViewModel.kt` | one-time unenrol | Bug 1, only if Q1 = unenrol |

## Steps
One slice per bug (see 00-plan Slices). Each: change, `assembleDebug` + `lint`, check on the emulator, commit.

## Conventions
kotlin-conventions; comments say why; strings in both `strings.xml` if any are added.

## Do not touch
Firestore/Storage rules, `google-services.json`, Diogo's files beyond these lines.
