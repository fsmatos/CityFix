---
name: verify
description: Quality gate before each commit: builds the debug app, runs Android lint and unit tests.
---

# Verify

Run, fastest first, stopping at the first failure:

1. `./gradlew assembleDebug`
2. `./gradlew lint`
3. `./gradlew testDebugUnitTest`

Report pass or fail per step with the failing output. If no Kotlin or Gradle files are staged, say so and skip. Slow suites (instrumented tests) belong to `/test-code`.
