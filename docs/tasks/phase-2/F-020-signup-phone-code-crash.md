# F-020: Sign-up "Send Code" crash

Owner: Francisco · Asked in chat on 2026-11-21 (`/bug-fix`).

## Bug
On the account setup phone step, tapping "Send Code" crashes the app. `requestCode()` in `FirebaseAuthRepository` always sets `requireSmsValidation(true)`, but since F-018 `sendPhoneCode` has no multi-factor session, so `PhoneAuthOptions.Builder.build()` throws `IllegalArgumentException`, uncaught in `AccountSetupViewModel`.

## Fix asked
- Apply `requireSmsValidation(true)` only to the MFA builder (sign-in hint and re-auth).
- On the sign-up path, handle `onVerificationCompleted` so the screen does not wait for the 60 s timeout.

## Acceptance checks
- "Send Code" on sign-up no longer crashes; the SMS arrives and the code links the phone.
- Instant verification / auto-retrieval on sign-up links the phone without typing a code.
- MFA sign-in and re-auth for pre-F-018 accounts still ask for the typed code.
- `./gradlew assembleDebug` and `./gradlew lint` pass.
