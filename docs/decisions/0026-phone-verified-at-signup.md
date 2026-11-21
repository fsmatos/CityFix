# 0026: Phone verified at sign-up, no SMS at login

Status: accepted (F-018, chosen by the person on 2026-11-20). Changes the MFA part of [0015](0015-session-role-navigation-mfa.md). Code detail: `docs/architecture/overview.md`.

## Context
- Brief I.2.B and II.2.B: authentication by e-mail/password "com duplo fator (MFA)".
- Since D-009 the phone was enrolled as a Firebase MFA factor. Firebase then asks for the SMS code at **every** sign-in and re-authentication; there is no setting to skip it.
- Testing showed this is tiring for citizens; the person asked for the SMS only at registration.

## Decision
- Account setup still verifies the e-mail and then the phone by SMS, but the phone is **linked** to the account (`PhoneAuthProvider` without a multi-factor session + `linkWithCredential`), not enrolled with `multiFactor.enroll`.
- Login and re-authentication ask only e-mail and password.
- `AuthAccount.hasVerifiedPhone` = a linked phone **or** an enrolled factor; `Session.NeedsPhoneVerification` replaces `NeedsSecondFactor`.
- Accounts created before F-018 keep their enrolled factor and the existing SMS step at login (`resolveSecondFactor`, `MfaScreen`), so they still work.
- New `AuthError.PHONE_IN_USE`: a phone can be linked to one account only.
- **Amended by F-020 (2026-11-21):** `requireSmsValidation(true)` is set only for the second-factor code (Firebase allows it only with a multi-factor session; F-018 had left it on the sign-up send, which crashed). On sign-up, a phone Firebase verifies by itself (`onVerificationCompleted`) is linked at once, without a typed code.

### Rejected
- Keep MFA on every login: matches the brief but was the complaint.
- MFA only for operators: half the change, two flows to explain.
- Unenrol the factor after login for old accounts: extra code for a few test accounts.

## Consequences
- **Deviation from the brief:** login is single factor; the report must say so and why.
- Testing several accounts needs a different phone number (or Firebase test number) per account.
- The Phone sign-in provider must stay enabled in the Firebase console.
- Old test accounts still get the SMS at login until they are recreated.
