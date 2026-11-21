# F-020: Sign-up "Send Code" crash
Type: bug · Task file: docs/tasks/phase-2/F-020-signup-phone-code-crash.md
Labels: none

## Problem
Since F-018 the phone is verified at sign-up without a multi-factor session, but the shared `requestCode()` still forces `requireSmsValidation(true)`. Firebase only allows that with a session, so sign-up cannot finish.

## Outcome
Sign-up sends the SMS and links the phone; MFA paths behave as before.

## Acceptance checks
- [ ] Sign-up "Send Code": no crash, code step opens, typed code links the phone.
- [ ] Auto-retrieval / instant verification on sign-up links the phone at once (no 60 s wait).
- [ ] Pre-F-018 account: sign-in and re-auth still send and ask for the typed code.
- [ ] `assembleDebug` and `lint` pass.

## Reproduction
1. Register a new citizen account; verify the e-mail; tap "I have verified".
2. On the phone step type a valid mobile (9 digits, starts with 9) and tap "Send Code".
3. The app closes. Logcat: `IllegalArgumentException: You cannot require sms validation without setting a multi-factor session` from `PhoneAuthOptions$Builder.build`, thrown in `FirebaseAuthRepository.requestCode` inside `AccountSetupViewModel.sendCode`'s coroutine.
Found by reading the code (no device run in this session); the person can confirm on a device.

## Root cause
`requestCode()` (FirebaseAuthRepository.kt:254) applies `requireSmsValidation(true)` to every builder. F-018 (decision 0026) changed `sendPhoneCode` to a plain phone verification with no `setMultiFactorSession`, so `build()` fails its precondition and throws synchronously. Nothing catches it, so `viewModelScope` crashes the app. Also, `onVerificationCompleted` is ignored, so on the sign-up path an instant verification (no `onCodeSent`) would wait 60 s and fail with CODE_NOT_SENT.

## Approach
- `requestCode(builder, mfa: Boolean)`: `requireSmsValidation(true)` only when `mfa` (sendSecondFactorCode, used by sign-in and re-auth).
- Sign-up (`mfa = false`): `onVerificationCompleted` resumes with the credential; `sendPhoneCode` links it (same code as `linkPhone`, pulled into a private `linkCredential`) and returns a new `CodeSendResult.Linked`.
- `AccountSetupViewModel`: `Linked` stays busy (session changes, redirect leaves, like a confirmed code); a link error is shown like in `confirmCode` (PHONE_IN_USE on the phone field).
- Login and CredentialChange ViewModels: `Linked -> Unit` branch (never happens on MFA paths), comment says so.
- Alternative rejected: a separate result type for sign-up only (more types for one case).

## Architecture impact
One new `CodeSendResult` case; no new files or dependencies.

## Failure modes
- Auto-retrieval arriving after `onCodeSent`: ignored (continuation already resumed); the user types the code as today.
- Link fails (phone in use, network): shown on the phone step.

## Risks and open questions
- Instant verification is hard to trigger on a test number; check by reading plus the normal SMS path.

## Out of scope
Rotation during send (existing timeout behaviour), any change to MFA accounts.

## Slices
1. Fix in repository + ViewModels (`fix(F-020): send sign-up phone code without MFA-only option`).
2. Docs (`docs(F-020): record sign-up code fix`).
