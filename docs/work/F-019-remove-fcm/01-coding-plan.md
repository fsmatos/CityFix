# F-019 coding plan

## Files
| Path | Change | Why |
| --- | --- | --- |
| `app/src/main/java/.../data/CityFixMessagingService.kt` | delete | FCM receiver |
| `app/src/main/java/.../data/PushTokens.kt` | delete | FCM token storage |
| `app/src/main/AndroidManifest.xml` | remove the `CityFixMessagingService` `<service>` and its comment | service gone |
| `app/src/main/java/.../CityFixApplication.kt` | remove `messaging`, `pushTokens` and their imports | container |
| `app/src/main/java/.../viewmodel/NotificationsViewModel.kt` | remove the `pushTokens.register` call; KDoc without "FCM token" | only the worker remains |
| `app/src/main/java/.../viewmodel/NotificationsViewModelFactory.kt` | KDoc: drop "PushTokens" | stale comment |
| `app/src/main/java/.../data/FirestoreUsersRepository.kt` | drop `"fcmTokens" to emptyList<String>()` | field no longer used |
| `app/src/main/java/.../viewmodel/TicketsViewModel.kt` | comment example: "fcmTokens" -> another profile field (e.g. phone) | stale comment |
| `app/build.gradle.kts` | remove `implementation(libs.firebase.messaging)` | dependency |
| `gradle/libs.versions.toml` | remove `firebase-messaging` | catalog |
| `firestore.rules` | only if Q1 = yes: drop `'fcmTokens'` from the owner allow-list and its comment | unused field |

`viewModelScope`/`launch` imports in `NotificationsViewModel` go if nothing else uses them.

## Interfaces
`NotificationsViewModel.onUserChanged(user)` keeps its signature; it now only calls `TicketCheckWorker.schedule` / `cancel`.

## Steps
1. **Slice 1:** all code, manifest, Gradle (and rules) changes above. Verify: grep for `messaging|PushTokens|fcm` in `app/` and `gradle/` returns nothing; `verify` (assembleDebug + lint). Device check: change a ticket's status as operator, see the citizen's notification and open it. Commit `refactor(F-019): remove FCM`.
2. **Slice 2:** docs (see 03-docs-plan). Commit `docs(F-019): record FCM removal`.

## Conventions
`kotlin-conventions`; comments stay short and point to F-019 / decision 0027 where something was removed on purpose.

## Do not touch
`TicketNotifier`, `TicketCheckWorker`, `NotificationPrefs`, `utils/Notifications.kt`, the `POST_NOTIFICATIONS` request in `MainActivity`, `google-services.json`, the google-services Gradle plugin (Auth, Firestore and Storage still need it).
