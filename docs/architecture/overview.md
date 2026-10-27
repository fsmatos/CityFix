# Architecture overview

Seeded from the factory manifest (A1-A4). Update as the app takes shape.

## Components
- One Android app, Kotlin, Jetpack Compose + Material3. minSdk 24, targetSdk 37. Package `pt.isec.amov.cityfix`.
- Single Gradle module `:app`.

## Layers (MVVM)
- `ui/screens/`: stateful screens (collect `StateFlow<UiState>`) and their stateless content composables; `ui/components/` reusable composables; `ui/navigation/` routes; `ui/theme/` theme.
- `viewmodel/`: ViewModels and their `UiState`.
- `data/`: repositories over Firebase Auth, Firestore, Storage, FCM; WorkManager workers for media upload.
- `model/`: entities, enums and pure rules (ticket lifecycle, 5-day reopen, 10 m grouping, 10 km anonymous radius, 20 m geofence).
- `utils/`: stateless helpers (image compression and thumbnails, distance maths).

## Domain model
Chosen model: **Option A, one ticket with several following citizens** (see [decision 0002](../decisions/0002-grouped-reports-single-ticket.md)). A report with the same category + situation within 10 m of an active ticket makes the citizen follow that ticket instead of creating a new one. There is no `Grupo` entity.

![Domain model, Option A](diagrams/CityFix_Dominio_OpcaoA.png)

Entities: `Utilizador` (nome, nif, email, telefone, foto?) specialised into `Municipe` (georreferenciacao, estadoRegisto, detecaoImpactos) and `Operador`; `Municipio` (nome, dominioEmail, centro, raioKm) with its `Categoria`s and operators; `Ticket` (descricao, situacao, alcance, localizacao, estadoAtual, dataCriacao) composed of `Mensagem`, `HistoricoEstado` (1..*) and `Anexo` (1..*, with thumbnail and upload state). Enums: `Situacao`, `Alcance`, `EstadoTicket`, `EstadoRegisto`, `TipoAnexo`, `EstadoUpload`.

Rejected alternative, kept for the technical report: [Option B, tickets grouped by a `Grupo`](diagrams/CityFix_Dominio_OpcaoB.png).

## UI foundation
- Theme: the course template trio in `ui/theme/` (`Color.kt`, `Theme.kt`, `Type.kt`). Light only (dark mode is not required by the brief): `CityFixTheme` has one `lightColorScheme` with primary `CityFixBlue`, secondary `CityFixTeal`, tertiary `CityFixOrange`. `Type.kt` stays the template. Screens read `MaterialTheme.colorScheme` and `MaterialTheme.typography`, never raw colours.
- `dynamicColor` defaults to `false`, the one change to the template: on Android 12+ dynamic colour would replace the CityFix colours with the wallpaper palette.
- `MainActivity` calls `enableEdgeToEdge` with `SystemBarStyle.light(TRANSPARENT, TRANSPARENT)` for status and navigation bars. The default style follows the device, so in device dark mode it would draw light icons over the light-only UI; the light style keeps them dark and readable.
- Strings: English default in `values/strings.xml`, Portuguese in `values-pt/strings.xml`, same snake_case keys; the device language picks the file, falling back to English. Composables use `stringResource(R.string.x)`, no literals. `app_name` is `translatable="false"`. Each task adds its strings with the screen that uses them, EN and PT in the same commit (lint `MissingTranslation` catches gaps).
- Sizes: `dp` for layout, `sp` for text (so text follows the font scale).
- Touch targets: 48dp through Material 3 components (`Button`, `IconButton`, ...), which enforce the minimum size; no custom size constants.
- `contentDescription`: meaningful icons use `stringResource(...)`; `null` only for decorative icons next to text that already says the same.
- Later (report): dark theme, in-app language selector (per-app locales), dynamic colour toggle in settings, custom container colours.

## External services
Firebase Auth (+MFA), Cloud Firestore (offline persistence), Firebase Storage, FCM; Android location/geofencing and sensors.

## Roles
Anonymous citizen (public, active issues only, no personal data), registered citizen, municipal operator.

## References
Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`.
