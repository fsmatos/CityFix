# F-020 review
Reviewed: `git diff 04c7b48..HEAD` (code commit 8e20138) against 00-plan, 01-coding-plan and the kotlin-conventions. Date: 2026-11-21.

## Verdict
No blocking findings. The fix follows the approved plan and removes the crash.

Checked:
- `requireSmsValidation(true)` is only added when `mfa` is true, and only `sendSecondFactorCode` passes `true`. `sendPhoneCode` passes `false`, so `build()` no longer throws without a multi-factor session.
- When sign-up gets `onVerificationCompleted` before `onCodeSent`, it returns `SmsOutcome.Verified`. `sendPhoneCode` then links it through the shared `linkCredential` and returns `CodeSendResult.Linked`. `linkPhone` is now a one-line delegate, and the link and reload logic did not change.
- The `CancellationException` rethrow before every `catch (e: Exception)` is still there (both catches in `linkCredential`). `requestCode` has no try/catch, so it needs no rethrow.
- The `when` blocks are exhaustive in all three ViewModels. In `AccountSetupViewModel`, `Linked -> Unit` stays busy, the same as `confirmCode`'s `null -> Unit`. `linkCredential` re-emits `_account`, so the redirect still happens.
- MVVM and stateless/stateful structure: no UI files changed. No new strings (`setup_error_phone_in_use` is reused, so PT is already covered). Anonymous data exposure, permissions, accessibility and orientation are not affected.
- The code stays plain and at class level. The private `SmsOutcome` is small and explained in KDoc. There is no defensive hardening.

## Blocking
None.

## Should-fix (non-blocking)
1. **PHONE_IN_USE after a Resend on the code step is silent.** `AccountSetupScreen` wires the code step's Resend to `sendCode` as well (`onResend = onSendCode`, line 135). If an auto-verified link fails with PHONE_IN_USE there, `sendCode` sets `phoneError`, but the CODE step does not show the phone field. The button turns enabled again and the user sees no message. This is rare (it needs instant verification on a resend), but the comment says "as in confirmCode" and the behaviour differs.
   Fix: in `sendCode`'s `AuthError.PHONE_IN_USE` branch, do what `confirmCode` does:
   ```kotlin
   AuthError.PHONE_IN_USE -> {
       setVerificationId(null)
       it.copy(isBusy = false, step = SetupStep.PHONE, code = "", phoneError = R.string.setup_error_phone_in_use)
   }
   ```
   (`setVerificationId` has a side effect, so call it before or outside the `update` lambda, or restructure it the same way `confirmCode` does.)

2. **Plan deviation not recorded.** 00-plan says the Login and CredentialChange ViewModels get a `Linked -> Unit` branch. The code uses `_uiState.update { it.copy(isBusy = false) }`, which is better because a `Unit` branch would leave the spinner on forever if it were ever reached. Fix: add one line to the `state.md` log saying so.

3. **Acceptance checks not yet evidenced.** `state.md` still shows Code as pending, with no `assembleDebug`/`lint` result and no device run of the reproduction. Fix: run `./gradlew assembleDebug lint`, then run the reproduction steps on a device (sign-up Send Code, then a pre-F-018 account's sign-in MFA code). Record the results in the Code row and log.

## Nit
1. In `requestCode`, `builder.setTimeout(...).setCallbacks(callbacks)` drops the returned builder and relies on the Firebase Builder mutating itself. That is correct, but someone reading it may wonder. Fix: keep the chain, e.g. `val options = builder.setTimeout(...).setCallbacks(callbacks).apply { if (mfa) requireSmsValidation(true) }.build()`.
2. The line in `sendSecondFactorCode` reads `is SmsOutcome.Verified -> CodeSendResult.Failure(AuthError.CODE_NOT_SENT)` with the comment "Not reached". That is fine. If you want it easier to explain at the defence, say why it stays a Failure (so the user can tap Resend) instead of throwing.
3. The new line in the `CodeSendResult.Linked` KDoc mentions "(F-020)". Other KDocs refer to decisions or tasks only sometimes. It is optional and fine as it is.
