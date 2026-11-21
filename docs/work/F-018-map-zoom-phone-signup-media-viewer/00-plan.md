# F-018: Map pinch zoom, phone check at sign-up only, full-size media
Type: fix · Task file: docs/tasks/phase-2/F-018-map-zoom-phone-signup-media-viewer.md

## Problem
- **Map:** `TicketMap` sits inside a `verticalScroll` column (ShowScreen, CitizenApprovalScreen). A two-finger gesture that moves vertically is taken by the column, so the map gets cancelled.
- **SMS:** the phone is enrolled as a Firebase MFA factor; Firebase then demands the code on every sign-in. There is no setting to skip it.
- **Media:** `MediaTile` in ShowScreen only shows a 96 dp thumbnail; nothing opens on tap.

## Outcome
Smooth map gestures; SMS once at setup; tap a tile to see the photo or play the video full screen.

## Approach
- **Map:** `TicketMap` reports "finger down / all fingers up" (`pointerInput` in the Initial pass on a wrapping Box). The screens keep `mapTouched` and use `verticalScroll(state, enabled = !mapTouched)`. Pure Compose, same idea as the maps-compose scrolling sample.
- **Phone:** setup sends the SMS with plain `PhoneAuthProvider` (no MFA session) and **links** the phone to the account (`linkWithCredential`) instead of `multiFactor.enroll`. Login is e-mail/password only. "Has verified phone" = linked phone **or** an old enrolled factor, so pre-F-018 accounts keep working (they still get the SMS at login, through the existing MFA screen).
- **Media:** new `ui/components/MediaViewer` (full-screen `Dialog`): photo with Coil `AsyncImage` (Fit, pinch zoom); video with `VideoView` + `MediaController` in `AndroidView`. Beyond class: the class has no video playback.

## Brief impact (decision 0026)
Brief I.2.B/II.2.B ask for "duplo fator (MFA)" at authentication. After this change login is single factor; the phone is only verified at sign-up. Chosen by the person (2026-11-20) for usability; record it in decision 0026 and the report as a known deviation.

## Failure modes
- A phone number can be linked to **one** account only: testing several accounts needs different numbers (or different Firebase test numbers). New error `PHONE_IN_USE`.
- The Phone sign-in provider must stay enabled in the console (it already is for MFA).
- Video before upload plays from the local path; a failed upload with a missing file shows the VideoView error dialog.
