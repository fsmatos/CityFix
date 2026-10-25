---
name: verify
description: "Quality gate before each commit: builds the debug app and runs Android lint."
---

# Verify

Run, fastest first, stopping at the first failure:

1. `./gradlew assembleDebug`
2. `./gradlew lint`

Report pass or fail per step with the failing output. If no Kotlin or Gradle files are staged, say so and skip.
