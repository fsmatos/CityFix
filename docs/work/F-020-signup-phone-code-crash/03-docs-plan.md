## Docs to update
- `docs/decisions/0026-phone-verified-at-signup.md`: amendment line (no SMS validation flag without a session; auto-verification links directly).
- `docs/architecture/overview.md`: one line in the auth section.

## Decisions to record
None new (fix within decision 0026).

## Report notes
- `requireSmsValidation` is valid only with a multi-factor session; MFA keeps it so the code is always typed.
- Sign-up accepts `onVerificationCompleted` to avoid a 60 s wait. Lesson: shared helpers must be rechecked when one caller changes (F-018).
