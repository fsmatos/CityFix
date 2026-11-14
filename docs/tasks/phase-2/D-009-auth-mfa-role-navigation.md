# D-009: Login, MFA, password recovery and role navigation

Owner: Diogo (`dev/Diogo`) · Layer: `data` / `ui` / `ui.navigation` · Phase 2 · Status: done (code, review, docs 2026-11-11 to 11-14; console set-up and device test 2026-11-14) · Target: 2026-11-13
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
- [x] Login with a test citizen and a test operator lands on different home screens. *Emulator 2026-11-14: active citizen -> dashboard with report and citizen menu; operator -> Operations panel; pending citizen -> waiting screen.*
- [x] A wrong second-factor code is rejected; the right one logs in. *Emulator 2026-11-14, typed by Diogo with the console test numbers.*
- [x] Closing and reopening the app keeps the session. *Emulator 2026-11-14: force-stop and relaunch as citizen and as operator keep the session and the role home.*
- [x] Password reset email arrives. *Emulator 2026-11-14: prefilled from Login, arrived in the inbox (confirmed by Diogo).*
- [x] Logged out, the report and operator routes cannot be reached. *Emulator 2026-11-14: after Log out no report button or menu; the guards on every destination were checked in review rounds 1-3.*

Status of the checks (2026-11-11): none ticked, because each needs the device or the Firebase console, and nothing has run there yet. Only `./gradlew assembleDebug` and `./gradlew lint` ran (0 errors, 18 warnings, the baseline). Pending, for Diogo (full list at the end of `docs/work/D-009-auth-mfa-role-navigation/review.md`):
- console steps (Identity Platform, SMS MFA, test numbers, SHA-1/SHA-256, three test users with `users/{uid}`; overview "Setup");
- login per role, and cold start as operator, setup user, pending citizen and account-problem user;
- wrong / right code at sign-in and at enrolment, with the `errorCode` strings seen in Logcat;
- verify and reset e-mails arriving; pending -> ACTIVE switching live;
- airplane-mode cold start after one online login, and a first install offline;
- logout from an operator placeholder (Back leaves the app);
- rotation and process death on each step; R2 (reCAPTCHA) and R3 (rotation during the send);
- PT strings, TalkBack labels, keyboard Next on Login, `enrolledFactors` after enrolment;
- since 2026-11-14 (session wiring, review round 3): the 10 km gate only for anonymous users (Log in opens Login), citizen own + public and operator all tickets, no stale list after Log out, a new report's `creatorId` = the citizen's uid;
- no rules deploy: the rules stay undeployed drafts (decision 0012 Q5, amended 2026-11-13).

Decision: [0015](../../decisions/0015-session-role-navigation-mfa.md). Work item: `docs/work/D-009-auth-mfa-role-navigation/`.

## References
Brief I, I.2.B, I.3.B, IV, V; decision 0010.
