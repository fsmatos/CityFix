Revision 2 (2026-11-01, strict brief). Replaces revision 1.

## Files
Paths are relative to `app/src/main/` unless they start with `gradle/` or `app/`. The Kotlin package root is `java/pt/isec/amov/cityfix/`.

| Path | Change | Why | Slice |
| --- | --- | --- | --- |
| `java/.../ui/screens/ShowScreen.kt` | New: `DetailAudience`, `ShowScreen`, private sections, preview data, previews | Ticket detail per audience (I.1.C, I.2.E, I.3.E) | 1-5 |
| `java/.../ui/components/CoarseLocationMap.kt` | New: `CoarseLocationMap(location, modifier)` | One place for the map library choice; placeholder in previews | 4, 6 |
| `java/.../utils/DateFormatting.kt` | Add `formatDateTime(epochMillis, locale)` | Date and time in the history and "Reported on" | 1 |
| `res/values/strings.xml`, `res/values-pt/strings.xml` | New keys under `<!-- Ticket detail -->`, after `<!-- Ticket list -->` | EN + PT | 1-4 |
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | One map library (Q7) | Real map | 6 |
| `AndroidManifest.xml` | `INTERNET` (+ `ACCESS_NETWORK_STATE` for osmdroid; + API key meta-data for Google, through a placeholder, never a committed key) | Map tiles | 6 |

String keys (EN / PT; PT says "ocorrência", D-005 review S1). Reused: `ticket_no_category`, `ticket_reported_on`, `status_*`, `situation_*`.
- Slice 1: `ticket_not_found` Ticket not found / Ocorrência não encontrada; `ticket_detail_description_title` Description / Descrição.
- Slice 2: `ticket_detail_history_title` Status history / Histórico de estados; `ticket_detail_history_empty` No status changes recorded. / Sem alterações de estado registadas.
- Slice 3: `ticket_media_title` Photos and videos / Fotografias e vídeos; `ticket_media_empty` No photos or videos. / Sem fotografias nem vídeos.; `ticket_media_photo` Photo / Fotografia; `ticket_media_video` Video / Vídeo; `ticket_media_unavailable` Not available yet / Ainda não disponível.
- Slice 4: `ticket_map_title` Approximate location / Localização aproximada; `ticket_map_caption` Approximate area (about 1 km) / Área aproximada (cerca de 1 km); `ticket_map_unavailable` Location not available / Localização indisponível; `cd_ticket_map` Map of the approximate area of the issue / Mapa da área aproximada da ocorrência.

## Interfaces
```kotlin
// utils/DateFormatting.kt (pure Kotlin: java.text, java.util only)
/** Medium date and short time in [locale], device time zone (java.text works on minSdk 24). */
fun formatDateTime(epochMillis: Long, locale: Locale): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(epochMillis))

// ui/screens/ShowScreen.kt
/** Who is looking at the detail; each value is a different view in the brief (I.1.C, I.2.E, I.3.E). */
enum class DetailAudience { PUBLIC, REGISTERED, OPERATOR }

/**
 * Ticket detail, or "Ticket not found" when [ticket] is null.
 * PUBLIC (brief I.1.C): header, media and a coarse map only. REGISTERED and OPERATOR also get
 * "Reported on", the description and the status history. Never reads author ids, follower ids,
 * messages, responsible ids, justifications or uploader ids; the anonymous projection happens
 * upstream (decision 0006).
 */
@Composable
fun ShowScreen(ticket: Ticket?, audience: DetailAudience, modifier: Modifier = Modifier)

// private, same file; modifier is always the first optional parameter and goes to the root only
@Composable private fun TicketHeader(ticket: Ticket, modifier: Modifier = Modifier)
@Composable private fun DescriptionSection(ticket: Ticket, locale: Locale, modifier: Modifier = Modifier)          // "Reported on" + description
@Composable private fun StatusHistorySection(history: List<StatusHistory>, locale: Locale, modifier: Modifier = Modifier)
@Composable private fun HistoryRow(entry: StatusHistory, locale: Locale, modifier: Modifier = Modifier)
@Composable private fun MediaSection(attachments: List<Attachment>, modifier: Modifier = Modifier)
@Composable private fun MediaTile(attachment: Attachment, modifier: Modifier = Modifier)

// ui/components/CoarseLocationMap.kt
/**
 * Map of the approximate area around [location] (already rounded for anonymous users, decision 0006):
 * fixed distant zoom, no gestures, a circle instead of a pin, no coordinate text.
 * Shows "Location not available" for the unset (0, 0). Draws a placeholder in previews.
 */
@Composable
fun CoarseLocationMap(location: Coordinates, modifier: Modifier = Modifier)
```

Caller (D-006, not part of this task):
```kotlin
composable<TicketDetailRoute> { entry ->
    val id = entry.toRoute<TicketDetailRoute>().ticketId
    ShowScreen(
        ticket = tickets.firstOrNull { it.id == id },   // vm.tickets collected above the NavHost
        audience = DetailAudience.PUBLIC,              // phase 1: no login
        modifier = Modifier.padding(innerPadding),
    )
}
```

## Steps
1. **Audience, header, not-found.**
   - `formatDateTime` in `utils/DateFormatting.kt`; `formatDate` unchanged.
   - Strings: slice-1 keys in both files.
   - `ShowScreen`:
     - `val locale = LocalConfiguration.current.locales[0]`.
     - `ticket == null`: `Box(modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Center)` with `Text(ticket_not_found, bodyLarge, onSurfaceVariant, TextAlign.Center)` (as the `ListScreen` empty state).
     - Else `Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), spacedBy(16.dp))` with `TicketHeader(ticket)`, then `when (audience) { PUBLIC -> Unit; REGISTERED, OPERATOR -> DescriptionSection(ticket, locale) }`. Exhaustive, no `else`.
   - `TicketHeader`: `Column(spacedBy(8.dp))` with category (`ifBlank` -> `ticket_no_category`, `headlineSmall`), situation label (`bodyMedium`), `StatusBadge(currentStatus)`.
   - `DescriptionSection`:
     - `if (createdAt > 0L)` show "Reported on" with `formatDateTime(createdAt, locale)` (`bodySmall`, `onSurfaceVariant`);
     - `if (description.isNotBlank())` show the heading `ticket_detail_description_title` (`titleMedium`, `semantics { heading() }`) and the text (`bodyLarge`, no `maxLines`).
   - Preview data (private), Q9: `previewTicket`:
     - id `preview-1`, "Public lighting", BREAKDOWN, PUBLIC, IN_PROGRESS;
     - `createdAt = 1_792_089_000_000L`, a 2-3 sentence description;
     - `location = Coordinates(40.21, -8.42)`;
     - `statusHistory`: OPEN at `createdAt`, INFO_REQUESTED at +1 day, IN_PROGRESS at +2 days (named args `status`, `dateTime` only);
     - `attachments`: `Attachment(id = "preview-a1", type = AttachmentType.IMAGE)` and `Attachment(id = "preview-a2", type = AttachmentType.VIDEO)`.
   - Previews (`private`, `CityFixTheme`, landscape `spec:width=411dp,height=891dp,orientation=landscape`):
     - `ShowScreenPublicPreview` (PUBLIC) and `ShowScreenFullPreview` (REGISTERED): each portrait, landscape, `locale = "pt"`, and PT + landscape;
     - `ShowScreenNotFoundPreview`: portrait, landscape, PT.
   - Commit: `feat(F-005): add ShowScreen with audience, header and not-found state`.
   - Verify:
     - assembleDebug passes; lint 0 errors and 13 warnings (no `MissingTranslation`, `ModifierParameter`, `UnusedResources` for new keys);
     - `ShowScreen.kt` has no match for `\.(creatorId|followerIds|messages|responsibleId|justification|uploadedById)\b`;
     - `\.description\b` is used only in `DescriptionSection`, which is called only from the non-PUBLIC branch;
     - `DateFormatting.kt` has no `import android`;
     - by hand: the PUBLIC previews show the header only; the REGISTERED previews add "Reported on <date, time>" (PT date in the PT preview) and the description.
2. **Status history (non-PUBLIC).**
   - Strings: slice-2 keys.
   - `HistoryRow`: `Row(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, CenterVertically, spacedBy(12.dp))` with `StatusBadge(entry.status)` and, if `dateTime > 0L`, `Text(formatDateTime(entry.dateTime, locale), bodyMedium)`.
   - `StatusHistorySection`: heading `ticket_detail_history_title` (`titleMedium`, `heading()`), then either `ticket_detail_history_empty` or `history.forEach { HistoryRow(it, locale) }`. Stored order: no `sortedBy`, `reversed` or `LazyColumn`.
   - In the REGISTERED/OPERATOR branch, after `DescriptionSection`: `HorizontalDivider()` and `StatusHistorySection(ticket.statusHistory, locale)`.
   - Previews:
     - `ShowScreenEmptyHistoryPreview`: REGISTERED, `Ticket(id = "empty", categoryName = "Pavement")`, EN and PT;
     - `ShowScreenPrivacyPreview`: PUBLIC with `SampleTickets.all[1]` (raw), and a KDoc saying why it exists.
   - Commit: `feat(F-005): show status history to registered citizens and operators`.
   - Verify:
     - build and lint at the baseline; the privacy grep is still empty; `\.statusHistory\b` appears only in the non-PUBLIC branch;
     - by hand: `previewTicket` in REGISTERED shows Open, Info requested, In progress (PT: Aberto, Pedido de informações, Em resolução) with dates and times;
     - by hand: the privacy preview shows "Public lighting", "Breakdown", "In progress" and nothing else so far (no description, no history, no ids);
     - by hand: the accessibility inspector shows one node per row and the titles as headings.
3. **Media tiles (all audiences).**
   - Strings: slice-3 keys.
   - `MediaTile`: `Column(modifier.width(120.dp).semantics(mergeDescendants = true) {}, horizontalAlignment = CenterHorizontally)`:
     - a 96dp `surface` box with `clip(shapes.small)` and the type icon (IMAGE: `painterResource(R.drawable.ic_image_placeholder)`; VIDEO: `Icons.Filled.PlayArrow`; `contentDescription = null` because the label below says it), tinted `onSurfaceVariant`;
     - the type label (`ticket_media_photo` / `ticket_media_video`, `bodyMedium`);
     - `ticket_media_unavailable` (`bodySmall`, `onSurfaceVariant`).
     - Exhaustive `when (attachment.type)`. Reads only `attachment.type`.
   - `MediaSection`: heading `ticket_media_title` (`heading()`), then `ticket_media_empty` or `Row(Modifier.horizontalScroll(rememberScrollState()), spacedBy(8.dp)) { attachments.forEach { MediaTile(it) } }`.
   - In `ShowScreen`, call `MediaSection(ticket.attachments)` after the header for PUBLIC, and after the description for non-PUBLIC (before the divider and history).
   - Commit: `feat(F-005): show photo and video tiles in ShowScreen`.
   - Verify:
     - build and lint at the baseline;
     - `ShowScreen.kt` has no `\.(file|thumbnail|uploadedById|uploadState)\b`;
     - by hand: `previewTicket` shows two tiles (Photo / Video, "Not available yet", PT "Fotografia" / "Vídeo", "Ainda não disponível"); the privacy and empty previews show "No photos or videos."
4. **Map section with placeholder.**
   - Strings: slice-4 keys.
   - `ui/components/CoarseLocationMap.kt`:
     - `Column(modifier, spacedBy(8.dp))` with heading `ticket_map_title` (`heading()`);
     - if `location == Coordinates()`: `Text(ticket_map_unavailable)`;
     - else a `Box(Modifier.fillMaxWidth().height(200.dp).clip(shapes.medium).background(surfaceVariant).semantics { contentDescription = cd_ticket_map }, Center)` with a `LocationOn` icon (`contentDescription = null`), then `Text(ticket_map_caption, bodySmall, onSurfaceVariant)`.
     - This slice: the box always holds the placeholder. Slice 6 adds `if (LocalInspectionMode.current) placeholder else real map`.
     - Private previews: a real-looking location and (0, 0), EN and PT.
   - In `ShowScreen`, call `CoarseLocationMap(ticket.location)` after `MediaSection` for every audience. This is the only `.location` read in `ShowScreen.kt`.
   - Commit: `feat(F-005): add approximate-location section to ShowScreen`.
   - Verify:
     - build and lint at the baseline;
     - `latitude|longitude` is absent from `ShowScreen.kt` and from both string files;
     - `\.location\b` appears exactly once in `ShowScreen.kt`;
     - by hand: previews show the placeholder and caption, and the empty preview shows "Location not available".
5. **Landscape two panes.**
   - `val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE`.
   - For a non-null ticket in landscape: `Row(modifier.fillMaxSize().padding(16.dp), spacedBy(24.dp))` with two panes, each `Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), spacedBy(16.dp))`:
     - left pane: `TicketHeader`, then `DescriptionSection` for non-PUBLIC, then `MediaSection`;
     - right pane: `CoarseLocationMap`, then `StatusHistorySection` for non-PUBLIC.
     - Keep one exhaustive `when (audience)` per pane.
   - Portrait and the not-found branch stay unchanged.
   - Commit: `feat(F-005): split ShowScreen into two panes in landscape`.
   - Verify: build and lint at the baseline (no `ModifierParameter`). By hand: the EN and PT landscape previews of both audiences show two panes, and each scrolls on its own.
6. **Real map (only after Gate 1 answers Q7).**
   - **Option B, osmdroid (recommended):**
     - catalog: `osmdroid = "6.1.20"` (confirm the last 6.1.x on Maven Central; the repo is archived) and `osmdroid-android = { group = "org.osmdroid", name = "osmdroid-android", version.ref = "osmdroid" }`; `implementation(libs.osmdroid.android)`;
     - manifest: `<uses-permission android:name="android.permission.INTERNET" />` and `ACCESS_NETWORK_STATE`.
     - In `CoarseLocationMap`, when `!LocalInspectionMode.current`, use `AndroidView(factory = { context -> ... }, onRelease = { it.onDetach() })`. In the factory:
       - `Configuration.getInstance().userAgentValue = context.packageName` (OSM tile policy);
       - `MapView(context)` with `setTileSource(TileSourceFactory.MAPNIK)`;
       - `setMultiTouchControls(false)` and `zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)`;
       - `minZoomLevel = 14.0`, `maxZoomLevel = 14.0`, `controller.setZoom(14.0)` and `controller.setCenter(GeoPoint(lat, lon))`;
       - `setScrollableAreaLimitDouble(BoundingBox(lat + 0.01, lon + 0.01, lat - 0.01, lon - 0.01))`;
       - overlays: a `Polygon` with `Polygon.pointsAsCircle(center, 600.0)`, fill = `primary.copy(alpha = 0.2f).toArgb()` read before the factory, outline = `primary`; and `CopyrightOverlay(context)`.
       - No pin, no location overlay, no `setOnTouchListener` (it triggers lint `ClickableViewAccessibility`).
     - The `Text` caption stays below the map.
   - **Option A, Google Maps Compose:**
     - catalog `maps-compose` (8.x) and `implementation`;
     - manifest `INTERNET` and `<meta-data android:name="com.google.android.geo.API_KEY" android:value="${MAPS_API_KEY}" />`, with the key injected from `local.properties` (never committed). How the key is created and stored is the person's step;
     - `GoogleMap(cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(lat, lon), 14f) }, uiSettings = MapUiSettings(zoomGesturesEnabled = false, scrollGesturesEnabled = false, rotationGesturesEnabled = false, tiltGesturesEnabled = false, zoomControlsEnabled = false))` with a `Circle(center, radius = 600.0, ...)`.
   - **Option C:** skip the slice; log the I.1.C gap.
   - Commit: `feat(F-005): show coarse OpenStreetMap in ShowScreen` (or `... Google Map ...`).
   - Verify:
     - assembleDebug passes;
     - lint: 0 errors; list any new warning IDs in `state.md`, accept `NewerVersionAvailable` like D-004, fix any others;
     - the build does not commit any key;
     - on an emulator with network, the `sample-1` detail (once D-006 is wired, or a temporary local call that is not committed) shows the map at a distant zoom with the circle and the attribution; it cannot zoom in; the page still scrolls when dragged outside the map. A drag that starts on the map may not scroll the page, which is acceptable: the map is 200dp high.
     - Rotate: no crash, and the map is released (no leak warning in Logcat).

Record each slice's outcome and any deviation in `state.md`.

## Conventions
- Stateless: no ViewModel, repository or `mutableStateOf`. Remembered state is limited to scroll states (and the camera state for option A).
- Privacy:
  - the screen never reads author, follower, responsible or uploader ids, messages or justifications;
  - `description` and `statusHistory` are read only in non-PUBLIC branches;
  - `location` is read once, to pass to the map;
  - no coordinates in any `Text` or string.
- Exhaustive `when (audience)` and `when (attachment.type)` with no `else`.
- Locale from `LocalConfiguration.current.locales[0]`. `utils/` stays pure Kotlin.
- EN + PT strings in the same commit; snake_case keys; `cd_` prefix for content descriptions.
- Theme colours and typography only; `dp` for layout.
- Accessibility:
  - headings on section titles;
  - merged history rows and tiles;
  - decorative icons `contentDescription = null` next to their label;
  - the map has `cd_ticket_map`.
- `modifier: Modifier = Modifier` is the first optional parameter of every composable and is applied once to its root.
- Previews: `private`, `CityFixTheme`, the shared landscape `spec:`, a PT landscape for each main state.
- New dependencies only in slice 6, through `gradle/libs.versions.toml`, after Gate 1.

## Do not touch
- Diogo's folders: `model/` (including `TicketPrivacy.kt`, `Attachment.kt`), `viewmodel/`, `data/` (`SampleTickets.kt` is only imported by a preview), `ui/navigation/`, and `utils/Distance.kt`.
- Shared UI: `ui/components/StatusBadge.kt`, `ui/components/TicketCard.kt`, `ui/theme/`, `ui/screens/ListScreen.kt`, `ui/screens/DashboardScreen.kt`, `MainActivity.kt`.
- `google-services.json`, any key or `local.properties` content.
- `docs/tasks/`, other tasks' work folders.
- Gradle files and the manifest only in slice 6, as approved at Gate 1.
