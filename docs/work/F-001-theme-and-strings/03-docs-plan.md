## Docs to update
- `docs/architecture/overview.md`: short "UI foundation" section: template theme trio (`Color.kt`, `Theme.kt`, `Type.kt`), light only, dynamic colour off; screens use `MaterialTheme.colorScheme` / `MaterialTheme.typography`; strings EN default plus `values-pt`, snake_case keys, `translatable="false"` for fixed names, each task adds EN and PT together; accessibility rules (contentDescription or `null` for decorative icons next to text, 48dp through M3 components, `dp` for layout and `sp` for text).
- `docs/tasks/phase-1/F-001-theme-and-strings.md`: tick the acceptance checks once verified (only if the task files are meant to be tracked that way; otherwise leave).
- `docs/work/F-001-theme-and-strings/state.md`: phase rows and log.

## Decisions to record
- No ADR needed: the choices follow the course template. The two deviations (light only, `dynamicColor = false`) are explained in the overview section and in a comment in `Theme.kt`. Write `docs/decisions/0003-ui-theme-and-localisation.md` only if the person asks.

## Report notes
- Technical solutions: Compose Material 3 theme from the course template with a CityFix light palette; resource-based localisation (`values` / `values-pt`, device language, fallback to English); accessibility through `contentDescription`, M3 components for 48dp targets and `sp` text.
- Options chosen: light only (dark mode not required); dynamic colour off so the brand colours are visible; no custom typography, `Dimens` object or extra dependencies, to stay close to the course material.
- Improvement opportunities: dark theme, in-app language selector (per-app locales), dynamic colour toggle in settings, custom container colours.
