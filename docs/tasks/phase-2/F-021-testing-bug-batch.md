# F-021: Bugs found in manual testing (2026-11-21)

Owner: Francisco · Asked in chat on 2026-11-21.

## Bugs reported
1. Logging in still asks for the SMS code.
2. Opening an incident photo and rotating the screen: the photo does not stay full screen.
3. Anonymous users see only public incidents with status "Open"; "In progress" ones are not counted as active.
4. In an incident's details, Home opens the dashboard; tapping the incidents tab again returns to the last incident's details, and the island highlight does not change.
5. Account setup: entering the phone and tapping "Send Code" closes the app.
6. Registration validation (operator) shows no thumbnail of the citizen's profile photo.
7. The message shown after creating an incident or joining a group is hard to see in dark mode.

## Acceptance checks
- Each bug above no longer happens on the emulator or device (or is explained as not a bug).
- `./gradlew assembleDebug` and `./gradlew lint` pass.
