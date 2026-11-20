# D-016: UI design system (Material 3, light and dark)

Branch: `design` (from `dev/Diogo` at 9c4bab4). Diogo asked for this branch in conversation on
2026-11-20; it departs from the one-dev-branch rule (CLAUDE.md "Workflow"), recorded in `state.md`.

## Goal
A modern, clean Material 3 look with a light and a dark scheme, applied to every screen,
**without changing logic or features**: no ViewModel, repository, worker, model or rule changes;
same strings (EN + PT), same `contentDescription`s, 48 dp targets, portrait and landscape,
88 dp list bottom padding for the FAB, and anonymous users still see no author or exact point.

## Part A: navigation (asked by Diogo on 2026-11-20, done first)
Bottom navigation bar (M3 `NavigationBar`), one set of tabs per role:
| Role | Tabs (first = home after login) |
| --- | --- |
| Active citizen | Dashboard, Active issues, Profile |
| Pending citizen | Dashboard, Active issues, Profile (home stays Registration pending) |
| Anonymous | Dashboard, Active issues (Log in icon stays in the top bar) |
| Operator | Operations panel, Approvals, Categories, Profile (Gate answer: 4 tabs) |
| Setup / account problem / loading | none |

- **Dashboard**: unchanged content. The report FAB (active citizen) shows only here now.
- **Active issues** (new `ActiveTicketsRoute`, `ActiveTicketsScreen`): the first 5 active tickets
  (`isActive`, the existing rule) of the same `tickets` list, and "See more" when there are more,
  which opens the existing full list (`TicketListRoute`, unchanged).
- **Profile** (`ProfileRoute`, new `ProfileOverviewScreen`, read-only): photo, name, role,
  registration status, e-mail, phone, NIF; "Edit details" opens the existing edit form, now
  `EditProfileRoute` (`ProfileScreen`, unchanged behaviour); for citizens, their tickets (the
  former My tickets list, same filter `isParticipant` and `effectiveStatus` badge). `MyTicketsRoute`
  and `MyTicketsScreen` are removed (content moved into Profile).
- **Top bar**: the 3-dot menu is removed; signed-in users get a Logout icon (Material "Logout",
  added as `res/drawable/ic_logout.xml` because `material-icons-core` lacks it) with the same
  `cd_log_out` label. No back arrow on a tab's own screen.
- **Bar visibility** (Gate answer): shown on every screen except Edit profile (and its change
  e-mail/password screens) and the login flow (Login, Forgot password, Register, Account setup),
  account problem, and while the 10 km gate covers the app.
- Tab switch: `popUpTo(homeFor(role)) { saveState = true }`, `launchSingleTop`, `restoreState`.
- New strings in both locales: `nav_*` tab labels, `top_bar_title_active`, `active_see_more`,
  `top_bar_title_edit_profile`, `profile_edit_details`; removed: `menu_my_tickets`,
  `menu_profile`, `menu_approvals`, `menu_categories` (unused after the menu goes).
- No ViewModel, repository or rule changes; ProfileViewModel is reused read-only by the overview.

## Part B: visual design (design system below, to be shown again for approval after Part A)

## What is there today
- `ui/theme/`: light scheme only (`primary` #1565C0, `secondary` #00695C, `tertiary` #BF360C),
  one typography override, no shapes, no spacing tokens, dynamic colour off.
- `MainActivity` forces light system bars; `themes.xml` parent is `Theme.Material.Light`.
- Screens already read `MaterialTheme.colorScheme`/`typography`; the only hard-coded colours are
  in `StatusBadge` (`CityFixGreen`, `Color.White`). Spacing is literal `dp` (mostly 8/16/12/4).

## Proposed design system

### 1. Colour (Material 3 tonal roles, brand seed CityFix blue #1565C0)
| Role | Light | Dark |
| --- | --- | --- |
| primary / onPrimary | #1565C0 / #FFFFFF | #A9C7FF / #003063 |
| primaryContainer / on | #D6E3FF / #001B3E | #00468C / #D6E3FF |
| secondary / onSecondary (teal) | #00695C / #FFFFFF | #82D5C8 / #003731 |
| secondaryContainer / on | #A0F2E3 / #00201C | #005047 / #A0F2E3 |
| tertiary / onTertiary (orange) | #A43B12 / #FFFFFF | #FFB59C / #5C1900 |
| tertiaryContainer / on | #FFDBCF / #380D00 | #832B05 / #FFDBCF |
| error / onError | #BA1A1A / #FFFFFF | #FFB4AB / #690005 |
| errorContainer / on | #FFDAD6 / #410002 | #93000A / #FFDAD6 |
| background = surface / onSurface | #F8F9FF / #191C20 | #111318 / #E1E2E8 |
| surfaceVariant / onSurfaceVariant | #E0E2EC / #43474E | #43474E / #C3C6CF |
| outline / outlineVariant | #73777F / #C3C6CF | #8D9199 / #43474E |
| surfaceContainerLowest..Highest | #FFFFFF #F2F3FA #ECEEF4 #E6E8EE #E1E2E8 | #0C0E13 #191C20 #1D2024 #282A2F #33353A |
| inverseSurface / inverseOn / inversePrimary | #2E3036 / #EFF0F7 / #A9C7FF | #E1E2E8 / #2E3036 / #1565C0 |

Extra role (not in M3): **success** for RESOLVED, light #2E7D32 / container #C8E6C9 / on #002204,
dark #8BD68E / container #1B5E20 / on #C8E6C9, exposed as `CityFixTheme.successColors` (a small
data class via `staticCompositionLocalOf`, picked by the same dark flag).

- The scheme follows the system setting (`isSystemInDarkTheme()`); dynamic colour stays off so the
  brand colours are kept (as today).
- System bars: `enableEdgeToEdge()` with the default auto style, so the icons follow the theme.
  `themes.xml` light parent + new `values-night/themes.xml` dark parent, so the window behind
  Compose (splash, rotation) is not white in dark mode.
- Google Maps tiles stay light in dark mode (a map style JSON is out of scope; noted as a limit).

### 2. Typography
Default system font (Roboto); no font download, so nothing new to explain or to fail offline.
M3 type scale with weights tuned for hierarchy:
- `headlineSmall` 24/32 SemiBold (screen hero titles: Login, Dashboard numbers header)
- `titleLarge` 22/28 SemiBold (top bar, dialog titles)
- `titleMedium` 16/24 SemiBold (card titles, section titles)
- `titleSmall` 14/20 Medium (section headers, in `primary`)
- `bodyLarge` 16/24, `bodyMedium` 14/20, `bodySmall` 12/16 (as M3)
- `labelLarge` 14/20 Medium (buttons), `labelMedium` 12/16 Medium (badges)

### 3. Shape
`Shapes(extraSmall 8, small 12, medium 16, large 20, extraLarge 28)` dp. Cards use `medium`,
text fields `extraSmall`, badges full pill (`CircleShape`), dialogs `extraLarge`; buttons keep the
M3 pill.

### 4. Spacing
`ui/theme/Dimens.kt`, a plain `object Dimens`: `xs 4`, `sm 8`, `md 12`, `lg 16`, `xl 24`,
`xxl 32`; `screenPadding 16`, `cardPadding 16`, `fabClearance 88`, `minTouchTarget 48`,
`formMaxWidth 480`, `contentMaxWidth 600`. Screens replace literal dp with these names (same
values where they are layout-critical, so portrait/landscape behave as now).

### 5. Base components (in `ui/components/`)
- `StatusBadge`: tonal pill (container colours instead of saturated fills), same labels.
  OPEN primaryContainer, INFO_REQUESTED tertiaryContainer, IN_PROGRESS secondaryContainer,
  RESOLVED success container, CLOSED surfaceContainerHighest / onSurfaceVariant.
- `TicketCard`: `Card` on `surfaceContainerLow`, `medium` shape, 16 dp padding, 72 dp rounded
  thumbnail on `surfaceContainerHighest`, badge top-right of the text column, date in
  `onSurfaceVariant`.
- New `SectionHeader(textRes)`: `titleSmall` in `primary`, 8 dp below (detail, operator, profile).
- New `SectionCard { }`: `OutlinedCard`-like grouped block (`surfaceContainerLow`, `medium`
  shape, 16 dp padding) for the field groups on detail/profile/operator screens.
- New `EmptyState(messageRes)`: centred `bodyLarge` in `onSurfaceVariant` for empty lists.
- Buttons: primary `Button` full width 48 dp+; secondary `OutlinedButton`; tertiary `TextButton`
  (as now, just consistent). `FormMessage` errors on `errorContainer`.
- Top bar: `TopAppBar` on `surface`, `surfaceContainer` when scrolled (M3 default colours).

## Out of scope
New features, icons from new libraries (`material-icons-extended`), animations beyond M3
defaults, map dark style, launcher icon, a user dark-mode toggle (follows the system).

## Risks
- Contrast: values above are M3 tonal pairs (AA for text); checked on the emulator in both modes.
- Francisco owns many `ui/screens` files: every changed file is listed in `state.md` for him.
- Deadline 2026-11-22: slices are independent commits; any slice can be dropped and the app still
  builds.
