---
name: kotlin-conventions
description: "Kotlin, Jetpack Compose, MVVM and Firebase conventions for CityFix. Use when planning, implementing, or reviewing app code."
user-invocable: false
---

# CityFix Kotlin conventions

Source of requirements: `docs/brief/AMov.ProjetoKotlin_PT.pdf`.

## Architecture
- MVVM with unidirectional data flow: `UiState` (immutable data class) out via `StateFlow`, events in via ViewModel functions.
- Stateful screen composable collects state (`collectAsStateWithLifecycle`) and passes it to a stateless content composable. Previews target the stateless one.
- `data/` repositories wrap Firebase Auth, Firestore, Storage, FCM; ViewModels depend on repository interfaces so they are unit-testable with fakes.
- ViewModels and their `UiState` live in `viewmodel/`; composables in `ui/screens/` and `ui/components/`; navigation in `ui/navigation/`; stateless helpers (image compression, distance maths) in `utils/`.
- `model/` holds entities, enums and pure rules: ticket state machine (Open, Info request, In progress, Resolved, Closed), 5-day reopen window, 10 m report grouping, 10 km anonymous radius around Pr. Republica, 20 m geofence.

- Domain model: Option A (see `docs/decisions/0002-grouped-reports-single-ticket.md`, diagram in `docs/architecture/diagrams/`). Reports within 10 m with the same category + situation do not create a ticket: the citizen follows the existing one. There is no `Grupo` entity; do not add one.

## Course stack (practical sheets P01-P07 and the ContactsApp example)
- No XML layouts: Compose only. Pure Kotlin entities in `model/`, free of UI frameworks.
- Navigation Compose 2.8+ with `@Serializable` route classes (Kotlin Serialization plugin applied in Gradle).
- ViewModel + `StateFlow` survives rotation; keep screen state there, not in the composable.
- Lists: `LazyColumn` with stable `key`s over stateless content; locale-aware sorting with `java.text.Collator` when sorting text.
- Media and location as taught: TakePicture contract with `FileProvider`, Coil 3 `AsyncImage`, FusedLocationProvider, Maps Compose/OSM.
- Adaptive UI: use window size / orientation to split layouts in landscape (P07), per screen.

## Firebase
- Firestore offline persistence stays on; reads and writes must work offline.
- Media: compress and create thumbnails locally, then upload via WorkManager with a network constraint. Never upload on the main thread.
- Auth: email/password with MFA and password reset by email. NIF unique per profile type; citizen registration stays pending until an operator validates it.
- Anonymous users: only public active issues; hide author data and exact coordinates (coarse map zoom).

## Android
- Runtime permissions: location (foreground then background), camera, notifications; rationale first, handle denial.
- Impact detection: accelerometer vertical axis, only when the user enabled it; suggest opening an issue, never open one silently.
- Strings in `res/values/strings.xml` (English) and `res/values-pt/strings.xml`. English is the default language; the extra language (PT) follows the device setting. Support portrait and landscape without losing state or clipping content. Accessibility: content descriptions, 48dp targets, sufficient contrast.

## Testing
- No automated tests in this project; verify by building, linting and running the app. Keep `model/` rules pure and ViewModels behind repository interfaces anyway.

## Delivery
- Before the ZIP: remove `.gradle`, `build`, `app/build`, `local.properties`. Include a PDF technical report.
