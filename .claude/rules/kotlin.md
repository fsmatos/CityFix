---
paths:
  - "app/**/*.kt"
---

# Kotlin rules (CityFix)

- MVVM: a `ViewModel` exposes immutable `StateFlow<UiState>`; composables never touch Firebase, WorkManager or sensors directly.
- Stateful/stateless split: each screen has a stateful `XxxScreen(viewModel)` that collects state and a stateless `XxxContent(state, onEvent...)` that is previewable and testable.
- Layers under `pt.isec.amov.cityfix`: `ui/` (`screens/`, `components/`, `navigation/`, `theme/`), `viewmodel/` (ViewModels and UiState), `data/` (repositories, Firebase, workers), `model/` (entities, enums, pure rules such as lifecycle transitions and 10 m grouping), `utils/` (stateless helpers). Do not add new top-level folders without asking.
- No hard-coded user-visible strings: use `strings.xml` (English default) plus `values-pt/strings.xml`.
- Every interactive element has a `contentDescription` or semantics label; touch targets at least 48dp; layouts must work in portrait and landscape.
- Anonymous users must never receive the author's personal data or exact coordinates; enforce in the repository layer, not only in the UI.
- Permissions are requested at the point of use with a rationale; handle denial and "don't ask again".
- Use coroutines with structured concurrency; no `GlobalScope`, no blocking calls on the main thread.
- Plain code over clever code: the authors must be able to explain every file at the defence.
