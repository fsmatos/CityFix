---
name: kotlin-conventions
description: Kotlin, Jetpack Compose, MVVM and Firebase conventions for CityFix. Use when planning, implementing, or reviewing app code.
user-invocable: false
---

# CityFix Kotlin conventions

Source of requirements: `docs/brief/AMov.ProjetoKotlin_PT.pdf`.

## Architecture
- MVVM with unidirectional data flow: `UiState` (immutable data class) out via `StateFlow`, events in via ViewModel functions.
- Stateful screen composable collects state (`collectAsStateWithLifecycle`) and passes it to a stateless content composable. Previews target the stateless one.
- `data/` repositories wrap Firebase Auth, Firestore, Storage, FCM; ViewModels depend on repository interfaces so they are unit-testable with fakes.
- `domain/` holds pure rules: ticket state machine (Open, Info request, In progress, Resolved, Closed), 5-day reopen window, 10 m report grouping, 10 km anonymous radius around Pr. Republica, 20 m geofence.

## Firebase
- Firestore offline persistence stays on; reads and writes must work offline.
- Media: compress and create thumbnails locally, then upload via WorkManager with a network constraint. Never upload on the main thread.
- Auth: email/password with MFA and password reset by email. NIF unique per profile type; citizen registration stays pending until an operator validates it.
- Anonymous users: only public active issues; hide author data and exact coordinates (coarse map zoom).

## Android
- Runtime permissions: location (foreground then background), camera, notifications; rationale first, handle denial.
- Impact detection: accelerometer vertical axis, only when the user enabled it; suggest opening an issue, never open one silently.
- Strings in `res/values/strings.xml` (English) and `res/values-pt/strings.xml`. Support portrait and landscape. Accessibility: content descriptions, 48dp targets, sufficient contrast.

## Testing
- Unit tests (JUnit) for `domain/` and ViewModels with fakes; Compose UI tests for stateless content composables.

## Delivery
- Before the ZIP: remove `.gradle`, `build`, `app/build`, `local.properties`. Include a PDF technical report.
