# D-005: Docs plan

## Docs to update
- `docs/architecture/overview.md`:
  - in "Layers", add to the `ui/navigation/` description: "and the stateless `MainTopAppBar` (D-005)";
  - add a short "Top bar (D-005)" subsection after "Navigation (D-004)" covering:
    - `MainTopAppBar` is stateless and needs no navigation library;
    - `TopBarScreen` has one value per route;
    - the table of controls per route (00-plan);
    - `material-icons-core` comes through the BOM at 1.7.8, the library's frozen last release, because `material3` 1.4 no longer brings icons;
    - the D-006 contract from 00-plan, "Architecture impact".
- `docs/tasks/phase-1/D-006-main-activity-nav.md`: no edit. The contract in `overview.md` is enough.
- Tell Francisco (in the push message or in person) that D-005 added the first `values-pt/strings.xml`, and that the `top_bar_*` and `cd_*` names are taken. Skip this if the merge in slice 2 already resolved it.

## Decisions to record
- None. These are local UI choices, explained in `overview.md` and the report notes. If Diogo wants a record, the next free number is `0007`.

## Report notes
Add a "D-005: MainTopAppBar" section to `docs/report/notes.md`:
- **Stateless top bar:** the bar receives the screen and `canNavigateBack`, and sends clicks out as callbacks. It can be previewed with no `NavController`, and `MainActivity` stays the only place that knows about navigation.
- **Enum vs plain flags:** the enum keeps the "which control on which route" rule in one file, and an exhaustive `when` makes a new screen a compile error until it has a title.
- **Back arrow from the back stack, not the route:** this is why `canNavigateBack` is separate from `screen`.
- **Icons:** `material3` 1.4 has no icons, so we use `material-icons-core`, not the much larger extended library. `KeyboardArrowUp`/`KeyboardArrowDown` replace `ExpandLess`/`ExpandMore`.
- **Accessibility and localisation:** content descriptions name the action, the toggle's description changes with its state, `IconButton` gives 48dp targets, and `AutoMirrored` flips the back arrow in right-to-left languages.
- **Improvement opportunities:**
  - hide the add button by role in the Auth phase;
  - move to Material Symbols vectors, as the icons library is no longer updated.
