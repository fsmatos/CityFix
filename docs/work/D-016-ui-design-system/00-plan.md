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

## Part B: visual design (revised 2026-11-20 after Diogo's detailed spec)

### Navigation update (done before Part B)
The bottom bar is a floating "island" (a074889): pill shape, card background and thin outline,
soft shadow, 24dp side and 12dp bottom margins, at most 480dp wide, icons only (outlined; filled
when selected) with the tab label as `contentDescription`, a discreet circle behind the selected
icon (content colour at 8% alpha, so darker in light and lighter in dark). It sits in the
Scaffold's `bottomBar` slot, so the content padding already keeps every screen's content above it;
the 88dp list padding stays for the report FAB.

### 1. Colour: neutral base + one accent (choose A, B or C)
| Token | Light | Dark |
| --- | --- | --- |
| background | #F6F6F4 | #141414 |
| surface (cards, island, top bar = background) | #FFFFFF | #1E1E1E |
| surfaceVariant (thumbnails, tonal) | #EEEEEB | #2A2A29 |
| outlineVariant (card outline) | #E4E4E0 | #2F2F2E |
| outline | #8A8A86 | #8E8E8A |
| onSurface | #1B1B1A | #ECECEA |
| onSurfaceVariant | #5E5E5A | #A6A6A2 |
| error / onError | #B3261E / #FFFFFF | #F2B8B5 / #601410 |

Accent (`primary`; `secondary` and `tertiary` map to the neutrals, so nothing else is coloured):
- **A Terracota**: light #B4502F (5.1:1 on white), dark #F0A58A (8.3:1 on #1E1E1E)
- **B Verde pinho**: light #0F766E (5.5:1), dark #5EC4B6 (7.9:1)
- **C Índigo**: light #4F46E5 (6.3:1), dark #A5B4FC (8.3:1)

The accent is used only for: primary buttons, the report FAB, links and text buttons, focus and
the selected state of form controls.

Status dots (8dp dot + neutral text; non-text contrast at least 3:1 on the card in both themes):
OPEN blue #3E7BD6 / #7FA8EC, INFO_REQUESTED violet #8E5BD0 / #B796EA, IN_PROGRESS amber #C98A12 /
#E2B54E, RESOLVED green #2E8B57 / #6CC892, CLOSED grey #8A8A86 / #8E8E8A. Kept in
`ui/theme/StatusColors.kt` (picked by the dark flag), not in the screens.

Light/dark follows the system (`isSystemInDarkTheme()`); dynamic colour stays off; system bar
icons follow the theme; `values-night/themes.xml` so the window is not white in dark mode.

### 2. Typography: Inter
Inter 400, 500 and 600 as static TTF files in `res/font/` (offline; SIL OFL 1.1, licence text in
`docs/`), one `FontFamily` in `ui/theme/Type.kt` set on every style of the M3 type scale; screens
only use `MaterialTheme.typography`. Weights: headline/title 600, titleSmall and labels 500, body 400.

### 3. Shape and spacing
Cards 16dp, thumbnails 12dp, buttons and island pill, dialogs 28dp (`ui/theme/Shape.kt`).
`ui/theme/Dimens.kt`: 4, 8, 12, 16, 24, 32; `screenPadding` 16, `fabClearance` 88,
`minTouchTarget` 48, `contentMaxWidth` 600, `formMaxWidth` 480.

### 4. Base components (`ui/components/`)
- `CityCard`: neutral surface, 1dp `outlineVariant` outline, 16dp corners, no elevation, optional
  56dp leading thumbnail or icon on `surfaceVariant`.
- `StatusLabel`: dot + neutral text (replaces the coloured `StatusBadge` chip; same strings).
- `ScreenState` helpers: `LoadingState`, `EmptyState` (icon + message), `ErrorState` (message +
  optional retry), used wherever a screen already has those states.
- Buttons: primary `Button` (accent), secondary `FilledTonalButton` on neutral, `TextButton` (accent
  text) for "See more" and minor actions; no strong outlines.
- Top bar: no background colour (`containerColor` = background, also when scrolled), title left,
  Logout right.
- Motion: M3 defaults plus short fades (`AnimatedContent`/`animateColorAsState`, 150-250 ms) on
  state changes and tab selection; no heavy animation.

### 5. Order
Theme + Inter + base components, then Active issues first (shown to Diogo before the rest), then
the remaining screens one per commit.

## Out of scope
New features, icons from new libraries (`material-icons-extended`), animations beyond M3
defaults, map dark style, launcher icon, a user dark-mode toggle (follows the system).

## Risks
- Contrast: values above are M3 tonal pairs (AA for text); checked on the emulator in both modes.
- Francisco owns many `ui/screens` files: every changed file is listed in `state.md` for him.
- Deadline 2026-11-22: slices are independent commits; any slice can be dropped and the app still
  builds.
