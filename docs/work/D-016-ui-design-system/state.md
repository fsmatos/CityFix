# D-016: UI design system (Material 3, light and dark)
Branch: design · Base: 9c4bab4 (dev/Diogo)
Remote: origin/design

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-20 | Asked in conversation (no task file); branch `design` asked by Diogo |
| Plan | done | 2026-11-20 | 00-plan Part A (bottom navigation) + Part B (design system), 01-coding-plan |
| Gate 1: plan approved | partial | 2026-11-20 | Part A approved (answers below); Part B to be shown again after Part A |
| Code | in progress | 2026-11-20 | Part A (navigation) f4f16f4; Part B waits for the design-system approval |
| Review | pending | | |
| Docs | pending | | |
| Pushed | done | 2026-11-20 | ef56ce4 to origin/design |
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
- 2026-11-20 Inter (Diogo: "Download Inter"): inter_regular/medium/semibold.ttf (Google Fonts
  static files, ~325 KB each) in `res/font`, `AppFontFamily` in `Type.kt`, licence in
  `docs/licenses/Inter-OFL.txt`. Verify pass (0 / 17). Commit 5b7e984 on `design`.
- 2026-11-20 Branch `ilha-navegacao` from `design` (Diogo asked for a new branch for the island
  change only). The island now floats over the content: drawn in CityFixApp's outer Box (not the
  Scaffold `bottomBar`), content full height (NavHost bottom padding 0 while it shows), FAB and
  snackbars lifted by `IslandClearance` (12 + 72 dp). `LocalBottomBarPadding` (navigation bar +
  clearance + 16 dp, + report button on the Dashboard; 0 without the island) added at the end of
  every scroll shown with the island: Active issues, full list, Profile, Dashboard, ticket detail
  (ShowScreen, both panes), Report (both panes), operator panel, approvals list and detail,
  Categories (list and its own FAB), `FormColumn`. Shadow 6 dp. No colour, type, logic or route
  change. Device (Iphone_Diogo, citizen, PT): full list (14 cards) scrolled mid-way passes under
  the island and the last card ends above it, light and dark; Dashboard FAB above the island.
  Verify: assembleDebug pass; lint 0 errors / 17 warnings.
  Francisco's files touched: `MainActivity.kt`, `CategoriesScreen.kt`, `CitizenApprovalScreen.kt`
  (check `git log`), `DashboardScreen.kt`, `ListScreen.kt`, `ReportScreen.kt`, `ShowScreen.kt`,
  `OperatorPanelScreen.kt`, `PendingCitizensScreen.kt` (bottom padding lines only).
- 2026-11-20 Diogo approved Active issues ("Continua com os restantes ecrãs"). `design` fast-
  forwarded to `ilha-navegacao` (20a28cd) so the remaining screens get the floating island; the
  two branches are equal at that point.
- 2026-11-20 Code, remaining screens (one commit each, verify 0 errors / 17 warnings every time):
  b3872db every OutlinedButton -> FilledTonalButton (neutral, no strong outline); 3aa6cf6
  Dashboard (`CityCard` stat cards, neutral "Ver ocorrências ativas") and full list (EmptyState,
  Dimens, max width, `animateItem`); 444533d detail (`SectionCard` per section in ShowScreen,
  TicketDetailScreen and OperatorTicketScreen; neutral message bubbles in MessageThread;
  EmptyState for "not found"); 5a7d9bf Report form groups in section cards; 94f463a Profile tab
  (LoadingState/ErrorState, CityCard); e500735 approvals list (CityCard with a person icon,
  Loading/Empty states), Categories states, operator panel spacing; 6b133c1 citizen approval
  (states, map section); fbf3b13 neutral icon on PlaceholderScreen; b2180e2 `PrimaryButton`
  (solid indigo, white text in both themes, `primaryContainer`) for the 20 filled buttons,
  because the dark `primary` (light indigo, needed for readable accent text) made them pastel.
  Login, MFA, Register, Forgot password, Account setup, Change credential, Edit profile and the
  gate only needed the theme, the tonal and primary buttons (no layout change).
- Device (citizen, PT): Dashboard light, detail (public view) light, Report dark, Profile dark
  after the PrimaryButton change. Not checked on device: auth screens (would need a log out),
  operator screens (operator login), landscape after the restyle, TalkBack.
- 2026-11-20 Device check, login and operator (emulator `Iphone_Diogo`, PT; operator signed in by
  Diogo): anonymous shows the 2-tab island and the Log in icon; Login light and dark (solid
  indigo button, light-indigo links in dark, no island); operator island with 4 tabs; Operations
  panel light and dark; Approvals list; citizen approval detail (Activate in indigo, map section);
  Categories (indigo switches and add button, the add button above the island); operator ticket
  detail in dark (section cards, status dots in the history, Resolve indigo next to a neutral
  Request info, Contacts and Messages cards). Fix found: the sort chip's arrow was indigo, now
  `onSurfaceVariant`. Verify pass (0 / 17). Not checked: Register, MFA screen, Forgot password,
  Account setup, landscape after the restyle, TalkBack.
- 2026-11-20 Push: verify passed on ef56ce4 (assembleDebug; lint 0 errors / 17 warnings); pushed
  `design` to `origin/design` (new remote branch). No PR, `main` and `dev/Diogo` untouched; the
  local `ilha-navegacao` branch (20a28cd, contained in `design`) was not pushed.
