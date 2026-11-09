# D-009: Login, MFA, password recovery and role navigation

Owner: Diogo (`dev/Diogo`) · Layer: `data` / `ui` / `ui.navigation` · Phase 2 · Status: to do · Target: 2026-11-13
Depends on: F-010 · Blocks: D-010, F-014, F-016 and everything role-specific

## Goal
Let citizens and operators log in with email/password plus a second factor, recover their password by email, and see the screens of their role.

## Why
- Brief I.2.B, I.3.B: email/password with MFA and credential recovery by email.
- Brief IV: Firebase Authentication, session cached on the device after the first login.
- Brief I: three profiles with different features.

## What to build
- **`data/AuthRepository`:** `currentUser: Flow<User?>` (Auth state + `users/{uid}` document, mapped by `role` to `Citizen`/`Operator`), `signIn`, `signOut`, `sendPasswordReset(email)`.
- **MFA** with the factor chosen at F-010 Gate 1: Firebase multi-factor (SMS or TOTP, needs Identity Platform; check the plan) handling `FirebaseAuthMultiFactorException` → resolver → code screen; plus enrolment of the second factor after the first login. If the plan rules it out, document the fallback (for example email verification + a one-time code by email) and agree it before coding.
- **Pending citizens:** a citizen with `registrationStatus = PENDING` can log in but sees a "waiting for validation" screen with only the anonymous features (decide at Gate 1).
- **Screens:** Login, MFA code, Forgot password; Log in / Log out in the top bar.
- **Role navigation:** a `SessionViewModel` above the `NavHost`; anonymous → dashboard/list/detail; citizen → plus report, my tickets, profile; operator → operations panel, approvals, categories, profile. Top bar actions and FAB change by role and screen (brief V).
- The report route requires a logged-in active citizen (today it is open to all).

## Files
New `data/AuthRepository.kt`, `data/UsersRepository.kt` (read part), `viewmodel/SessionViewModel.kt`, `ui/screens/LoginScreen.kt`, `MfaScreen.kt`, `ForgotPasswordScreen.kt`; `MainActivity.kt`, `ui/navigation/*`, strings EN/PT.

## Acceptance checks
- [ ] Login with a test citizen and a test operator lands on different home screens.
- [ ] A wrong second-factor code is rejected; the right one logs in.
- [ ] Closing and reopening the app keeps the session.
- [ ] Password reset email arrives.
- [ ] Logged out, the report and operator routes cannot be reached.

## References
Brief I, I.2.B, I.3.B, IV, V; decision 0010.
