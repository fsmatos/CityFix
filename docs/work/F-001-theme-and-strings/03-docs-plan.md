## Docs to update
- `docs/architecture/overview.md`: short "UI foundation" section: theme files, `Dimens`, string key naming, EN default plus `values-pt`, rule that each task adds EN and PT together.
- `docs/tasks/phase-1/F-001-theme-and-strings.md`: tick the acceptance checks once verified (only if the task files are meant to be tracked that way; otherwise leave).
- `docs/work/F-001-theme-and-strings/state.md`: phase rows and log.

## Decisions to record
- Optional ADR `docs/decisions/0003-ui-theme-and-localisation.md`: custom palette with dynamic colour off by default (contrast checked by calculation), no custom font, device-language localisation only (no in-app picker), `values-night` window theme instead of adding the Material Components library. Write it only if the person wants it; the choices are small.

## Report notes
- Technical solutions: Compose Material 3 theme with explicit light/dark schemes; resource-based localisation via `values`/`values-pt`; lint `MissingTranslation` plus manual checks (device in Portuguese, dark mode, font scale, 48dp by code review, contrast by calculation) for translation and accessibility.
- Options chosen: dynamic colour off for brand consistency and controllable contrast; no extra dependencies.
- Improvement opportunities: in-app language selector (AppCompat per-app locales), dynamic colour toggle in settings, high-contrast theme.
