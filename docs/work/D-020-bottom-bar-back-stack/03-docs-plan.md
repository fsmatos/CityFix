## Docs to update
- `docs/architecture/overview.md`, "App shell (D-006)" navigation bullets (around the
  `launchSingleTop` bullet, line ~339): one bullet for the bottom bar since D-020: the selected tab
  is derived from `navController.currentBackStack` (nearest tab screen from the top, `NavGraph`
  skipped), not from a saved "last tab"; tapping the selected tab pops to its root with
  `popBackStack(tab.route, inclusive = false)`, otherwise the usual save/restore `navigate`. Note
  the kept edge case: LIST opened from the Dashboard highlights Active (it is in `ACTIVE.screens`).
- Manual test checklist (wherever Diogo keeps it): mark the I.3.E navigation issue fixed.

## Decisions to record
None. Local navigation fix inside D-016's bottom bar; no new trade-off beyond what the overview
bullet explains.

## Report notes
- Bottom navigation with per-tab back stacks: why the standard `popUpTo(start) { saveState }` +
  `restoreState` pattern needs two extra rules: highlight from the real back stack (screens opened
  from a tab are in no tab's set) and an explicit re-select that pops to the tab root (otherwise
  the same save/restore brings the stack straight back).
- State survives rotation and process death because the highlight is computed from the
  NavController's own saved back stack, so no extra saveable state is needed.
- Improvement opportunities: nested graphs per tab (each route then belongs to exactly one tab),
  scroll-to-top when re-selecting a tab already on its root.
