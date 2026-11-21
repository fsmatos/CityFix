## Files
| Path | Change | Why |
| --- | --- | --- |
| `data/AuthRepository.kt` | add `data object Linked : CodeSendResult` + KDoc | sign-up auto-verification result |
| `data/FirebaseAuthRepository.kt` | `requestCode(builder, mfa)`; handle `onVerificationCompleted` when `!mfa`; private `linkCredential`; update class/function KDoc | root cause |
| `viewmodel/AccountSetupViewModel.kt` | `Linked` branch; link errors as in `confirmCode` | finish without the code step |
| `viewmodel/LoginViewModel.kt`, `CredentialChangeViewModel.kt` | `Linked -> Unit` branch | exhaustive `when` |

## Interfaces
- `CodeSendResult.Linked` (only from `sendPhoneCode`).
- Private: `requestCode(builder, mfa: Boolean)` resumes with a small private result (sent id, failure, or credential); `sendSecondFactorCode` passes `true`, `sendPhoneCode` passes `false`.

## Steps
1. Slice 1: the changes above. Commit `fix(F-020): send sign-up phone code without MFA-only option`. Verify: `assembleDebug`, `lint`; on device, the reproduction no longer crashes and the SMS links the phone.
2. Slice 2: docs per 03-docs-plan.

## Conventions
Keep the `CancellationException` rethrow before each `catch (e: Exception)`. Plain code, comments explain why.

## Do not touch
MFA resolver logic, timeout value, UI composables, strings (reuse existing messages).
