# D-009 docs plan

## Docs to update
- **`docs/architecture/overview.md`**
  - **New subsection "Auth and session (D-009)"** under "External services", after "Handover (F-010)". It covers:
    - the repositories (interfaces + Firebase implementations, created in `CityFixApplication`);
    - the `Session` sealed type and its order of checks;
    - `SessionViewModel` above the `NavHost` (activity scope, `flatMapLatest` over the users listener);
    - the offline behaviour (Auth persistence + the Firestore cache, `ProfileUnavailable`);
    - the MFA sign-in and enrolment flows with the API names;
    - the `Activity` hand-off and the rotation limit (R3);
    - whether reCAPTCHA appeared on the emulator (R2);
    - the `AuthError` mapping and the `errorCode` strings seen.
  - **"Roles":** replace the one line with a table:

    | Role (`AccessRole`) | Home | Menu | Add button | Expand toggle |
    | --- | --- | --- | --- | --- |

    One row per role, from `homeFor` and `menuItemsFor`. Add a line on how the guard works (hidden entry points, `RequireAccess`, the redirect on a role change with the saveable last role).
  - **"Navigation (D-004)":** the new routes, `ForgotPasswordRoute(email)` carrying only the e-mail, why the second-factor code is a step of `LoginRoute` (the resolver is not serialisable), and that the placeholder routes belong to F-014, D-011, D-012 and D-013.
  - **"Top bar (D-005)":**
    - the controls table, with new columns for account action and menu and the new `TopBarScreen` rows;
    - the "Requirement for the Auth phase" bullet marked done by D-009 (add only for active citizens);
    - the new parameters and previews.
  - **"App shell (D-006)":**
    - state table rows for `SessionViewModel`, `session`, `handledRole` (`rememberSaveable`), and the screen ViewModels of the new routes;
    - `canExpandCards` now comes from `session.canExpandCards`;
    - the loading gate;
    - the report destination is guarded;
    - correct the `canExpandCards` row ("D-009 replaces it").
  - **"Firebase (F-010)":** the MFA bullet confirmed at D-009's Gate; the rules-deploy state (deployed on <date> with the person's OK, or still not deployed).
  - **"Setup":** the console checklist for MFA (Identity Platform upgrade, SMS MFA, test numbers, SHA-1/SHA-256, SMS region policy), and that test numbers and codes are never committed. Add a link to the test-account table, without UIDs, e-mails or phone numbers.
  - **"Handover (F-010)":** mark the D-009 operator e-mail verification item as handled (the Account setup verify step). Add a "Handover (D-009)" list:
    - **D-010:** after registration, the session goes to `NeedsEmailVerification`, and the app routes to Account setup by itself. Registration must create `users/{uid}` (with `nifs`) before or right after sign-up; until it exists, the user sees Account problem. For operators, the document is created only after verification (F-010 handover), so D-010 must order "verify -> create doc"; the session shows `ProfileMissing` in between. Agree whether D-010 creates the operator's document from the setup flow.
    - **D-011:** `ProfileRoute` placeholder; the Log out action already exists.
    - **D-012 / D-013:** `ApprovalsRoute`, `CategoriesRoute`, `OperatorPanelRoute` placeholders, guarded for OPERATOR.
    - **F-014 (Francisco):** `MyTicketsRoute` placeholder, guarded for CITIZEN. The uid comes from `Session.SignedIn.user.id`. `DetailAudience` mapping needs the unprojected source (decision 0007).
    - **F-011 (Francisco):** `reportTicket` needs the creator's uid, from the session; the report route is reachable only by `AccessRole.CITIZEN`.
    - **F-016 (Francisco):** FCM token storage in `users/{uid}.fcmTokens` after login (`Session.SignedIn`).
- **`docs/tasks/phase-2/D-009-auth-mfa-role-navigation.md`:** status "done" and the acceptance boxes ticked with what was run. Diogo's file.
- **`docs/decisions/0011-firebase-plan-notifications-close-maps.md`:** point 6, "Diogo confirms at D-009's Gate", becomes "confirmed at D-009 Gate 1 on <date>". If R2 forced `setAppVerificationDisabledForTesting`, add that as a consequence. Francisco's record: a one-line amendment only, and tell him.
- **`docs/decisions/0006-anonymous-projection-phase-1.md`:** one amendment line saying the `canExpandCards` flag now comes from the role (Q7 answer). Point to 0013.
- **`docs/tasks/phase-2/D-010-registration.md`:** one line under "What to build" pointing to the D-009 handover (session-driven Account setup; operator document only after verification). Diogo's file.
- **`CLAUDE.md`:** no edit. If the person wants it, suggest adding "MFA test numbers and test-account e-mails stay out of git" to the non-negotiable rules.

## Decisions to record
- **0013: Session, role navigation and SMS MFA flow** (new; 0012 is D-008's lifecycle record). Format: Status, Context, Decision, Rejected, Consequences.
  - **Decision:**
    - the `Session` sealed type in an activity-scoped `SessionViewModel` above the `NavHost` (Q3);
    - auth and profile flows separate, combined in the ViewModel (deviation from the task's `currentUser: Flow<User?>`);
    - MFA mandatory for both roles, with verify e-mail -> enrol phone in Account setup (Q1, Q8);
    - pending citizens get a waiting home with anonymous features (Q2);
    - guarding by hidden entry points + `RequireAccess` + a redirect on role change (Q4);
    - offline from Auth persistence + the Firestore cache, with no extra store (Q5);
    - login as routes, the code as a step (Q6);
    - `canExpandCards` for operators only (Q7);
    - console-seeded test accounts (Q9);
    - the `Activity` passed through the ViewModel call, never stored;
    - `requireSmsValidation(true)`;
    - first phone hint only (Q13).
  - **Rejected:**
    - per-role graphs;
    - a `startDestination` that follows the session;
    - a `CompositionLocal` session;
    - a DataStore role cache;
    - TOTP;
    - optional MFA;
    - debug seeding code;
    - `setAppVerificationDisabledForTesting` (unless R2 forced it; then record that it is debug-only).
  - **Consequences:**
    - the rotation-during-send limit;
    - login needs the network;
    - one factor per user, so a lost phone means an operator/console reset;
    - MFA is enforced only in the client unless the rules check `sign_in_second_factor` (Q1 sub-question answer);
    - the placeholders that later tasks replace.
- If the person answers yes to the Q1 sub-question: update decision 0010 Consequences with the `sign_in_second_factor` check (Francisco's record; tell him).

## Report notes
Add a "D-009" section to `docs/report/notes.md`:
- **Technical solutions:**
  - the sealed `Session` driving navigation;
  - `flatMapLatest` from the Auth account to the `users/{uid}` listener (logout cancels the listener);
  - the redirect keyed on the role with a saveable "last role", so rotation does not reset the back stack;
  - `RequireAccess` as a second layer behind hidden buttons, with the Firestore rules as the real enforcement;
  - the MFA flows (`FirebaseAuthMultiFactorException` -> `MultiFactorResolver` -> `PhoneAuthProvider.verifyPhoneNumber` -> `PhoneMultiFactorGenerator.getAssertion` -> `resolveSignIn` / `enroll`);
  - callbacks wrapped in `suspendCancellableCoroutine` with a timeout;
  - an opaque `SecondFactorChallenge` keeps Firebase types out of the ViewModels;
  - the e-mail verification + token refresh needed by the operator rule.
- **Options chosen and why:**
  - SMS with test numbers, not TOTP (defence logistics, cost);
  - mandatory MFA;
  - console-seeded accounts until D-010;
  - pending citizens get anonymous features;
  - expanded cards only for operators;
  - no extra session store, relying on the Firebase caches.
- **Limits:**
  - login needs a network;
  - rotation while the code is being sent can lose the callback (timeout + resend);
  - reCAPTCHA on non-Play builds (record what was seen);
  - one second factor per user, so there is no self-service recovery;
  - MFA enforced client-side unless the rules check the claim;
  - real SMS cost and the region policy;
  - the placeholder screens.
- **Improvements:**
  - a second factor or TOTP as a backup;
  - the server-side `sign_in_second_factor` check;
  - deep-link-safe guards;
  - a splash screen API instead of the progress gate;
  - SMS retriever autofill;
  - a re-authentication flow for sensitive actions (D-011 password change).
- **Verification:** what ran on which AVD (Google Play image or not), which acceptance checks passed, and the rules deploy date if it happened. Never include test numbers, codes, UIDs or e-mails.
