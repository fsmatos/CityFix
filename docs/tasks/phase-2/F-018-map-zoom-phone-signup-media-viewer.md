# F-018: Map pinch zoom, phone check at sign-up only, full-size media

Owner: Francisco (`dev/Francisco`) · Layer: `ui` / `data` · Phase 2 · Status: to do · Target: 2026-11-21
Depends on: F-012, D-009, F-009

## Goal
Three corrections found while testing:
1. The exact map on a ticket (signed in) does not pinch-zoom smoothly; only the +/- buttons work well.
2. The SMS code is asked on every login; we want it only once, at registration.
3. A photo or video on a ticket cannot be opened full size.

## Acceptance checks
- [ ] Two-finger zoom and one-finger pan on the ticket map work without the page scrolling.
- [ ] A new account verifies its phone by SMS once during setup; later logins ask only e-mail/password.
- [ ] Accounts created before F-018 (with MFA enrolled) still log in.
- [ ] Tapping a photo opens it full screen; tapping a video plays it full screen; back closes it.
