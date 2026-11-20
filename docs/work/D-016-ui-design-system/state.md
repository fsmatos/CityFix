# D-016: UI design system (Material 3, light and dark)
Branch: design · Base: 9c4bab4 (dev/Diogo)
Remote: none yet

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-20 | Asked in conversation (no task file); branch `design` asked by Diogo |
| Plan | done | 2026-11-20 | 00-plan Part A (bottom navigation) + Part B (design system), 01-coding-plan |
| Gate 1: plan approved | partial | 2026-11-20 | Part A approved (answers below); Part B to be shown again after Part A |
| Code | in progress | 2026-11-20 | Part A (navigation) f4f16f4; Part B waits for the design-system approval |
| Review | pending | | |
| Docs | pending | | |
| Pushed | | | |
| Merged | | | |

## Log
- 2026-11-20 Start: deviation from CLAUDE.md "Workflow" (one dev branch per person): Diogo asked
  for a separate branch `design` from `dev/Diogo`, so the design work can be kept or dropped as a
  whole before the 2026-11-22 deadline.
- 2026-11-20 Scope change: Diogo added a navigation rework (bottom bar, three tabs, Logout icon
  instead of the 3-dot menu), done before the visual pass.
- 2026-11-20 Gate 1, Part A: operator gets 4 tabs (panel, approvals, categories, profile);
  anonymous gets 2 tabs (dashboard, active issues); the bar is hidden only on Edit profile and the
  login flow. Part B (design system): "show it again later", after the navigation is done.
- 2026-11-20 Code Part A (main conversation): `MainBottomBar.kt` (`BottomTab`, `tabRootScreens`),
  `bottomTabsFor(role)` replaces `menuItemsFor` in `RoleAccess.kt`; top bar without the overflow
  menu, Logout icon from `res/drawable/ic_logout.xml` (Material "Logout" path; `cd_log_out`).
  New `ActiveTicketsRoute`/`ActiveTicketsScreen` (first 5 `isActive` tickets, "See more" ->
  `TicketListRoute` when there are more than 5) and `ProfileOverviewScreen` on `ProfileRoute`
  (details, "Edit details" -> new `EditProfileRoute` = the old `ProfileScreen`, citizen tickets
  with the My tickets filter and `effectiveStatus`). `MyTicketsRoute` and `MyTicketsScreen.kt`
  removed (Francisco's F-014 file). `ProfileScreen`: photo cache-key code moved into
  `rememberProfilePhotoModel` (shared, same behaviour). Report FAB on the Dashboard only.
  Strings: `nav_*`, `top_bar_title_active`, `top_bar_title_edit_profile`, `active_see_more`,
  `profile_edit_details`, `profile_my_tickets` added; `menu_my_tickets`, `menu_profile`,
  `menu_approvals`, `menu_categories`, `top_bar_title_my_tickets`, `cd_more_options` removed
  (unused). Deviation: the build needs `JAVA_HOME` = `C:\Program Files\Java\jdk-21` on this
  machine (Android Studio's jbr path does not exist). Verify: assembleDebug pass; lint 0 errors /
  17 warnings (baseline). Commit f4f16f4.
- 2026-11-20 Device check Part A (emulator `Iphone_Diogo`, active citizen already signed in, PT):
  Dashboard tab with FAB and Logout icon; Active issues shows 5 cards and "Ver mais"; Profile
  shows photo, name, role, e-mail, phone, NIF, status, "Alterar detalhes" and the empty tickets
  text; Edit details hides the bar and has a back arrow; Back twice returns to the Dashboard;
  landscape keeps the bar and the FAB. Not checked: operator tabs, anonymous tabs, pending
  citizen, TalkBack.
- Francisco's files changed in Part A (tell him): `MainActivity.kt` (shared), `MyTicketsScreen.kt`
  (deleted), both `strings.xml` (shared).
- 2026-11-20 Spec revised by Diogo (detailed brief and a reference image): floating icon-only
  island bar, neutral palette with one accent, status dots, transparent top bar, Inter font,
  screen states, Active issues first and shown before the rest. Palette: Diogo's reference image
  instead of options A/B/C; accent indigo #4338CA (report button, white text, both themes, 16dp
  corners, 52dp high, "+" icon, weight 500). "Ver mais": neutral text button. The branch stays
  `design` (the brief's `redesign-ui` was an example).
- 2026-11-20 Code: island bar a074889 (outlined icons, filled when selected, labels as
  contentDescription, 56dp tabs, 8% content-colour circle; in the Scaffold `bottomBar` slot, so
  content padding keeps lists above it). Theme 2eaa488: `Color.kt`, `StatusColors.kt`
  (`LocalStatusColors`, `CityFixTheme.statusColors`), `Theme.kt` (light/dark by system,
  `primaryContainer` = indigo for the FAB, neutral secondary/tertiary), `Type.kt` (one
  `AppFontFamily`, weights 400/500/600; system font until Inter is added), `Shape.kt`,
  `Dimens.kt`, `values-night/themes.xml`, default `enableEdgeToEdge()`; components `CityCard`,
  `ScreenStates.kt` (Loading/Empty/Error), `StatusBadge` now dot + neutral text (same name and
  callers), `TicketCard` on `CityCard`; top bar on the background colour. Active issues 2d9ee77
  (EmptyState, Dimens, neutral "Ver mais", `animateItem`). Verify: assembleDebug pass; lint 0
  errors / 17 warnings. Device: light and dark screenshots of Active issues (citizen, PT).
- Open: Inter files not downloaded (permission asked, not answered); Active issues waits for
  Diogo's look before the other screens.
