# F-010 coding plan

**Prerequisite for every slice:** the person has put `app/google-services.json` in place (console work, Approach in 00-plan). Claude does not read, edit or stage that file. Before each commit, run `git check-ignore -v app/google-services.json` and check that `git status --porcelain` does not list it.

## Files
| Path | Change | Why |
| --- | --- | --- |
| `gradle/libs.versions.toml` | Versions `firebaseBom = "35.1.0"`, `googleServices = "4.5.0"`, `coroutinesPlayServices = "1.11.0"`. Libraries `firebase-bom`, `firebase-auth`, `firebase-firestore`, `firebase-storage`, `firebase-messaging` (no version, BoM-managed), `kotlinx-coroutines-play-services`. Plugin `google-services` (`com.google.gms.google-services`) | Brief III/IV libraries through the catalog |
| `build.gradle.kts` (root) | `alias(libs.plugins.google.services) apply false` | Same pattern as the other plugins |
| `app/build.gradle.kts` | `alias(libs.plugins.google.services)` in `plugins`. `implementation(platform(libs.firebase.bom))` plus the four Firebase libraries and coroutines play-services | Reads `google-services.json`, adds the SDKs |
| `app/src/main/java/pt/isec/amov/cityfix/CityFixApplication.kt` | Four `by lazy` Firebase properties. `firestore` applies the persistent cache settings. KDoc updated | One place that creates the Firebase instances (task, decision 0009 pattern) |
| `firestore.rules` (new, repo root) | Draft rules for the 0010 layout | Task; not deployed |
| `storage.rules` (new, repo root) | Draft rules for the 0010 Storage paths | Task; not deployed |
| `firebase.json` (new, repo root) | `{"firestore": {"rules": "firestore.rules"}, "storage": {"rules": "storage.rules"}}`, with no project id | Lets a later, approved deploy use the drafts |
| `docs/decisions/0010-firestore-schema.md` (new) | Schema decision, content from 00-plan "Schema" after Diogo's review | Acceptance check |
| `docs/decisions/0011-firebase-plan-notifications-close-maps.md` (new, if Q12 accepted) | The four Gate decisions as answered at Gate 1 | Phase-2 README |

Not touched: `app/google-services.json` (person only, ignored), `AndroidManifest.xml` (the Firebase manifests merge on their own), `model/`, `viewmodel/`, `ui/`, `data/`.

## Interfaces
`CityFixApplication` after slice 2 (shape, not final code):

```kotlin
class CityFixApplication : Application() {
    val ticketsRepository: TicketsRepository by lazy { LocalTicketsRepository() }   // unchanged until F-011
    val attachmentStorage: AttachmentStorage by lazy { AttachmentStorage(this) }   // unchanged

    val auth: FirebaseAuth by lazy { Firebase.auth }
    val firestore: FirebaseFirestore by lazy {
        Firebase.firestore.apply {
            firestoreSettings = firestoreSettings {
                setLocalCacheSettings(persistentCacheSettings { })
            }
        }
    }
    val storage: FirebaseStorage by lazy { Firebase.storage }
    val messaging: FirebaseMessaging by lazy { Firebase.messaging }
}
```

- Imports come from the main artifacts (`com.google.firebase.Firebase`, `com.google.firebase.auth.auth`, `com.google.firebase.firestore.firestore`, `...firestoreSettings`, `...persistentCacheSettings`, `com.google.firebase.storage.storage`, `com.google.firebase.messaging.messaging`). Do not use `-ktx` artifacts (removed in BoM 34).
- Leave the cache size at the default (100 MB) unless Gate 1 says otherwise.
- Consumers (later tasks): D-009 `AuthRepository(app.auth, app.firestore)`, F-011 `FirestoreTicketsRepository(app.firestore, app.auth)`, F-013 `app.storage`, F-016 `app.messaging`. The names `auth`, `firestore`, `storage` and `messaging` become the contract with Diogo once this is pushed.

## Steps

### Slice 1: build dependencies and plugin
- Before the change, save `./gradlew :app:dependencies --configuration debugRuntimeClasspath` to the scratchpad. After the change, run it again and diff the two.
- Add the catalog entries, the root `apply false` and the `:app` plugin and dependencies (keep the `dependencies` block alphabetical, as today).
- No Kotlin code uses them yet.
- Commit: `build(F-010): add Firebase BoM, google-services plugin and coroutines play-services`.
- Verify:
  - `./gradlew assembleDebug` passes **with** the json present. `processDebugGoogleServices` runs (it is visible in the build output).
  - `./gradlew lint` passes. Record every new warning id in `state.md` (expected: none, since the versions are the latest).
  - The dependency diff is recorded in `state.md`: `kotlinx-coroutines-*`, `androidx.core`, lifecycle, `kotlin-stdlib`, and Compose `runtime`/`ui` must stay within the limits in the overview "Dependencies".
  - The merged manifest (`app/build/intermediates/merged_manifest/debug/.../AndroidManifest.xml`) now has `INTERNET`. Note it for the docs.
  - If R1 (AGP 9) or R2 (Kotlin metadata) fails: apply the fallback in 00-plan Risks, record the version used and why, and do not change AGP or Kotlin.
- Optional proof (do not commit): with the json temporarily renamed, `assembleDebug` fails at `processDebugGoogleServices` with "File google-services.json is missing". Record the message in `state.md` for the setup docs, then restore the file.

### Slice 2: Firebase instances and the persistent Firestore cache
- Add the four properties to `CityFixApplication` and update its KDoc: one container for the repositories **and** the Firebase instances; settings applied once before first use.
- **Temporary smoke test, never committed:**
  - In `MainActivity.onCreate`, `lifecycleScope.launch { ... }` with `try/catch`. Log tag `F010`.
  - It writes `app.firestore.collection("debug").document("f010-smoke").set(mapOf("at" to System.currentTimeMillis())).await()`, then `get().await()`, and logs the data, `snapshot.metadata.isFromCache` and `app.firestore.firestoreSettings.cacheSettings`.
  - Run 1 is online: write and read succeed, and the settings print `PersistentCacheSettings`.
  - Run 2 is in airplane mode: the read returns the document with `isFromCache = true`. The write is queued and has no ack. That is fine, but the `await()` on `set` would hang, so in run 2 use `get(Source.CACHE)` only.
  - Record both results in `state.md`.
  - Then `git restore app/src/main/java/pt/isec/amov/cityfix/MainActivity.kt` (Diogo's file, only touched temporarily) and delete `debug/f010-smoke` in the console (the person).
- Commit: `feat(F-010): create Firebase instances with persistent Firestore cache`.
- Verify:
  - `assembleDebug` and `lint` pass.
  - `git diff --cached` contains only `CityFixApplication.kt`.
  - `git diff --cached | grep -i "f010-smoke\|debug\"" ` is empty.
  - `MainActivity.kt` is unchanged from HEAD.
  - The app still starts and shows the dashboard from `LocalTicketsRepository`.

### Slice 3: rules drafts, firebase.json, decision 0010 (and 0011)
- Write `firestore.rules` (`rules_version = '2'`) with helper functions `isSignedIn()`, `userDoc()`, `isOperator()`, `isActiveCitizen()`, `isParticipant(ticket)`, and one `match` per path in the 00-plan schema table:
  - deny by default;
  - `publicTickets`, `categories`, `municipality`: `allow read: if true`;
  - `nifs/{id}`: `allow get: if isSignedIn()`, `allow create: if request.resource.data.uid == request.auth.uid`, no update or delete;
  - follow update: `request.resource.data.diff(resource.data).affectedKeys().hasOnly(['followerIds', 'participantIds'])`, with the new lists equal to the old ones plus `request.auth.uid`;
  - `ticketLocations` per Q8 (only if accepted).
- Write `storage.rules` (`rules_version = '2'`) for the `tickets/{ticketId}/{file}` read and write rules (media read through `firestore.exists(...)` on `publicTickets`, else participants and operators via `firestore.get`) and `users/{uid}/photo.jpg`.
- Write `firebase.json` (no project id).
- Write `docs/decisions/0010-firestore-schema.md` in the decision-record format (Status, Context, Decision, Rejected, Consequences), with the Gate 1 answers to Q8 to Q11. Write 0011 if Q12 is accepted.
- **No `firebase deploy`.** The person may paste the rules into the console Rules Playground to check the syntax without publishing.
- Commit: `docs(F-010): draft Firestore and Storage rules and decision 0010`.
- Verify:
  - `assembleDebug` and `lint` still pass (no code change).
  - The rules contain no project id or key.
  - Each row of the schema table has a matching `match` block (self-review against the table).
  - Record in `state.md` whether the person ran the Playground check, and its result.

## Conventions
- Catalog naming as today: kebab-case aliases, `version.ref` for versioned entries, and no version on BoM-managed libraries (as `androidx-compose-ui`).
- Plugins: root `apply false`, applied in `:app`, as `kotlin-compose` and `kotlin-serialization` are (AGP 9 built-in Kotlin, no `kotlin-android`).
- Coroutines: `await()` from play-services. No `GlobalScope`, and no blocking `Tasks.await` on the main thread (`.claude/rules/kotlin.md`).
- No Firebase types in `model/` (decision 0003). `GeoPoint` and `Timestamp`, if ever used, stay in `data/`.
- Commits: Conventional Commits with `F-010`, no attribution lines.

## Do not touch
- `app/google-services.json`: never read, edit or stage it (CLAUDE.md).
- Firestore or Storage rules in the console, and `firebase deploy`: the person's OK is needed.
- `model/`, `viewmodel/`, `ui/`, `data/` and `AndroidManifest.xml`. `MainActivity.kt` only temporarily for the smoke test, restored before the commit.
- AGP and Kotlin versions. If R1 or R2 bites, change the Firebase/coroutines versions, not these.
- `main`: no push or merge into it.
- Diogo must have `google-services.json` **before** he merges this branch, or his build fails at `processDebugGoogleServices`. Tell him when F-010 is pushed.
