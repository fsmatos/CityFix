## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/pt/isec/amov/cityfix/ui/components/AttachmentViewer.kt` | new | Full-screen photo/video viewer dialog |
| `app/src/main/java/pt/isec/amov/cityfix/ui/screens/ShowScreen.kt` | edit | `MediaSection` remembers the opened attachment; `MediaTile` clickable |
| `app/src/main/res/values/strings.xml`, `values-pt/strings.xml` | edit | Close, error, tile click labels |

## Interfaces
`@Composable fun AttachmentViewer(attachment: Attachment, onDismiss: () -> Unit)`

## Steps
1. Slice 1: viewer + click. Commit `fix(D-017): play videos and open photos in the ticket detail`. Verify: `./gradlew assembleDebug lint`, then manual checks from 00-plan on the phone.

## Conventions
kotlin-conventions: stateless component, strings in resources, no hard-coded text, 48 dp touch targets, contentDescription on the close button.

## Do not touch
Model, repositories, Firestore/Storage rules, `AttachmentGallery`, `AttachmentImage`.
