# Architecture overview

Seeded from the factory manifest (A1-A4). Update as the app takes shape.

## Components
- One Android app, Kotlin, Jetpack Compose + Material3. minSdk 24, targetSdk 37. Package `pt.isec.amov.cityfix`.
- Single Gradle module `:app`.

## Layers (MVVM)
- `ui/`: stateful screens (collect `StateFlow<UiState>`) and stateless content composables.
- `data/`: repositories over Firebase Auth, Firestore, Storage, FCM; WorkManager workers for media upload.
- `domain/`: models and pure rules (ticket lifecycle, 5-day reopen, 10 m grouping, 10 km anonymous radius, 20 m geofence).

## External services
Firebase Auth (+MFA), Cloud Firestore (offline persistence), Firebase Storage, FCM; Android location/geofencing and sensors.

## Roles
Anonymous citizen (public, active issues only, no personal data), registered citizen, municipal operator.

## References
Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`.
