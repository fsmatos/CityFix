# F-018 coding plan

## Slice 1: map gestures (`fix(F-018): let the ticket map take pinch gestures`)
- `ui/components/TicketMap.kt`: new param `onTouchChange: (Boolean) -> Unit = {}`; wrap `GoogleMap` in a Box with `pointerInput { awaitEachGesture { awaitFirstDown(Initial) -> true; wait until no pointer pressed -> false } }`. Coarse map unchanged (gestures off).
- `ShowScreen.kt`: `var mapTouched by remember`; pass `!mapTouched` as `enabled` to both `verticalScroll` calls of the location side; thread the lambda through `LocationPane` to `TicketMap`.
- `CitizenApprovalScreen.kt`: same.

## Slice 2: phone at sign-up only (`fix(F-018): verify the phone once at sign-up`)
- `data/AuthRepository.kt`: `AuthAccount.hasSecondFactor` -> `hasVerifiedPhone`; `sendEnrolmentCode` -> `sendPhoneCode`; `enrolSecondFactor` -> `linkPhone`; new `AuthError.PHONE_IN_USE`. Doc comments updated.
- `data/FirebaseAuthRepository.kt`: `sendPhoneCode` without `setMultiFactorSession`; `linkPhone` = `user.linkWithCredential(PhoneAuthProvider.getCredential(...))`, then reload and re-emit `account` as today; `toAccount()` uses `phoneNumber != null || enrolledFactors.isNotEmpty()`. Sign-in/re-auth MFA paths kept for old accounts (comment says so).
- `data/AuthErrors.kt`: `FirebaseAuthUserCollisionException` with code `ERROR_CREDENTIAL_ALREADY_IN_USE` -> `PHONE_IN_USE`.
- `viewmodel/Session.kt`, `SessionViewModel.kt`: `NeedsSecondFactor` -> `NeedsPhoneVerification`; comment.
- `viewmodel/AccountSetupViewModel.kt`: new names; `PHONE_IN_USE` shown under the phone field. Error -> string mapping gets the new case.
- Strings EN/PT: `PHONE_IN_USE` message; setup texts that say "two-step / MFA" reworded to "verify your phone".

## Slice 3: full-size media (`feat(F-018): open ticket photos and videos full screen`)
- New `ui/components/MediaViewer.kt`: `Dialog(usePlatformDefaultWidth = false)`, black background, close button. Photo: `AsyncImage` Fit + `detectTransformGestures` zoom (1x-5x). Video: `AndroidView { VideoView }`, `setVideoPath(file)`, `MediaController`, start on prepared; `onRelease` stops playback.
- `ShowScreen.kt`: `var openAttachment by rememberSaveable`-free `remember`; `MediaTile` clickable when `file` is not blank (`onClickLabel` for TalkBack); show `MediaViewer` when set.
- Strings EN/PT: close, "Open photo", "Play video".

## Checks
`assembleDebug` + `lint` before each commit. On device: pinch on ticket map; new account setup + logout/login without SMS; old account login; photo and video full screen, rotate, back.
