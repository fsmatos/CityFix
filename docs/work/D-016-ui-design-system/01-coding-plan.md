# D-016 coding plan

Each slice: edit, `./gradlew assembleDebug`, `./gradlew lint` (baseline 0 errors / 17 warnings),
one commit `style(D-016): ...` on `design`. Only `ui/`, `res/` and `MainActivity` theme lines change.

1. **Theme**: `Color.kt` (light, dark, success), `Theme.kt` (`darkTheme` param, schemes, success
   local), `Type.kt`, new `Shape.kt`, new `Dimens.kt`; `themes.xml` + `values-night/themes.xml`;
   `MainActivity.enableEdgeToEdge()` default styles. Previews get a dark variant
   (`uiMode = UI_MODE_NIGHT_YES`) where touched.
2. **Base components**: `StatusBadge`, `TicketCard`, new `SectionHeader`, `SectionCard`,
   `EmptyState`; `AccountFields`/`FormMessage`, `MessageThread`, `AttachmentGallery`,
   `LocationDeniedMessage`.
3. **Navigation**: `MainTopAppBar` (colours, title style).
4. **Citizen browse**: `DashboardScreen`, `ListScreen`, `MyTicketsScreen`.
5. **Ticket detail**: `ShowScreen`, `TicketDetailScreen`.
6. **Report**: `ReportScreen`.
7. **Auth and gates**: `LoginScreen`, `MfaScreen`, `RegisterScreen`, `ForgotPasswordScreen`,
   `AccountSetupScreen`, `ChangeCredentialScreen`, `PendingValidationScreen`,
   `LocationGateScreen`, `PlaceholderScreen`.
8. **Profile**: `ProfileScreen`.
9. **Operator**: `OperatorPanelScreen`, `OperatorTicketScreen`, `CategoriesScreen`,
   `CitizenApprovalScreen`, `PendingCitizensScreen`.
10. **Check**: emulator light/dark, portrait/landscape on a few screens; record in `state.md`.

Rules per slice: no change to function signatures used by ViewModels/navigation, no new strings
unless both locales get them, keep every `contentDescription` and `semantics` modifier, keep the
88 dp bottom `contentPadding` on list screens.
