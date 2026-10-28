# D-002: Review

Reviewer: reviewer agent · Date: 2026-10-28 · Range: `git diff c2f9198..HEAD` (commits 25c2835..aa2cc9e; the code diff is the same from state.md's Base 3c4fe6d, which only adds the plan docs)

## Verdict
**Approve with should-fix.** There are no blocking findings. The code matches `01-coding-plan.md`, and the Gate 1 meaning of "active" (OPEN, INFO_REQUESTED, IN_PROGRESS) is applied through the one `Ticket.isActive` allow-list in `publicActive()`, `findNearby()` and `stats()`. The one exception is the repair-time average (S1). `model/` and `utils/` are pure Kotlin. The only imports are `pt.isec.amov.cityfix.utils.haversineMeters` in `model/` and `kotlin.math.*` in `utils/`, and `Math.toRadians` is `java.lang`, not Android. No attribution lines appear in the commits.

Re-run on 2026-10-28:
- `./gradlew assembleDebug`: BUILD SUCCESSFUL.
- `./gradlew testDebugUnitTest --rerun`: `TicketsListTest` 17/17 and `ExampleUnitTest` 1/1 pass, with 0 failures.

Checked and correct:
- **Haversine.** The formula is correct: `a = sin²(Δφ/2) + cos φ1 cos φ2 sin²(Δλ/2)` and `d = 2R·atan2(√a, √(1−a))`, with R = 6 371 000 m. An independent check (awk, same formula) gives exactly 9.000 m and 11.000 m for east-west offsets at the anchor latitude, so the 9 m / 11 m tests are not sitting on a rounding edge.
- **(0, 0) handling.** `isUnset()` compares exactly with `0.0`. In Kotlin, `-0.0 == 0.0` is true for a non-null `Double`, so a negative zero cannot slip through. Both the query and the candidate tickets are checked, and both cases are tested.
- **Stats maths.** `activeRatio` is guarded by `totalTickets == 0`, so it cannot be NaN. The average uses `Long` maths and is computed only when `durations` is non-empty, so there is no division by zero and no `average()` NaN. Tickets with `createdAt <= 0` or a negative duration are skipped.
- **Defensive copies.** `tickets` is `private`. `rawList` uses `toList()`, and `getAllSorted()`/`publicActive()` use `filter`/`sortedWith`, so each returns a new list. `Ticket` is a data class of `val`s with read-only `List` fields, so the shallow copy is enough.
- **Null-safe lookups.** The code uses `firstOrNull`, `lastOrNull` and `minByOrNull`, with no `!!` and no `first()`/`last()`.
- **Lambda shadowing.** In `findNearby`, the explicit `ticket ->` lambda parameter means `categoryId`/`situation` refer to the function parameters, so the comparison is correct.

## Blocking
None.

## Should-fix

**S1. A reopened ticket that is active again still counts in the average repair time.** This is the one place where Gate 1's "active" is not applied consistently.
- What happens: `stats()` takes the last RESOLVED entry of *any* ticket. Take a ticket with history OPEN -> RESOLVED -> OPEN (reopened within 5 days) and `currentStatus = OPEN`. It is counted in `activeTickets` *and* adds a repair time to `averageRepairTimeMillis`/`resolvedTickets`. So the dashboard reports a "repair" for a problem it also calls not yet repaired.
- Q8's reason ("after a reopen, the repair really ended at the final resolution") only holds once the ticket is resolved again.
- No test covers this case. The mixed-list test's reopened ticket is CLOSED.
- Fix, in `TicketsList.stats()`:
  ```kotlin
  val durations = tickets.mapNotNull { ticket ->
      if (ticket.isActive || ticket.createdAt <= 0) return@mapNotNull null
      ...
  ```
  - Update the `resolvedTickets` KDoc in `DashboardStats.kt` to read "Tickets that entered the average: not active, with a RESOLVED history entry".
  - Add a test: a ticket with `status = OPEN` and history OPEN, RESOLVED, OPEN counts in `activeTickets` but leaves `resolvedTickets` and the average unchanged.
  - If the person prefers the current behaviour, record it as a decision in 0005 instead (docs phase), with the reason.

**S2. The distance tests never exercise the longitude term of Haversine, and they are circular.**
- The problem:
  - `north()` moves along a meridian, so `Δλ = 0` and the `cos φ1 cos φ2 sin²(Δλ/2)` term is always 0.
  - Both `north()` and the 1° test use `111 195 m/°`, which is `EARTH_RADIUS_METERS·π/180` by definition. They check the threshold and the constant, not the formula.
  - A bug such as a missing `cos`, degrees passed to `cos`, or swapped lat/lon arguments would still pass all 17 tests.
- Fix: add to `TicketsListTest.kt`:
  ```kotlin
  private fun east(from: Coordinates, meters: Double) = Coordinates(
      from.latitude,
      from.longitude + meters / (111_195.0 * kotlin.math.cos(Math.toRadians(from.latitude))),
  )

  @Test fun findNearby_finds_ticket_9m_east() { /* ticket at east(anchor, 9.0) -> found */ }
  @Test fun findNearby_ignores_ticket_11m_east() { /* east(anchor, 11.0) -> null */ }

  @Test
  fun one_degree_of_longitude_at_60N_is_about_55597m() {
      assertEquals(55_596.9, haversineMeters(60.0, 0.0, 60.0, 1.0), 1.0)
  }
  ```
  The awk check of the same formula gives 55 596.934 m, which is half a degree of latitude, as cos 60° = 0.5 predicts.

**S3. The "copies" and ordering acceptance checks are only half proven.**
- The 00-plan check says: "Changing a list returned by `rawList` or `getAllSorted()` does not change the collection."
- `rawList_is_a_copy` proves the opposite direction: an old snapshot does not change after `add`. That is useful, but it is not the stated check.
- `getAllSorted()` has no test at all: neither its order (`createdAt` descending, `id` tie-break) nor its copy.
- `publicActive_keeps_only_public_active_tickets` compares a `Set`, so the newest-first order of the anonymous list (brief I.1.B, F-003's list) is not checked.
- Fix: add two tests.
  ```kotlin
  @Test
  fun getAllSorted_is_newest_first_then_id_and_a_copy() {
      val list = TicketsList(listOf(
          ticket("b", createdAt = 1_000L), ticket("a", createdAt = 1_000L), ticket("c", createdAt = 2_000L),
      ))
      val sorted = list.getAllSorted()
      assertEquals(listOf("c", "a", "b"), sorted.map { it.id })
      (sorted as MutableList<Ticket>).clear()   // the returned copy is an ArrayList
      assertEquals(3, list.rawList.size)
  }
  ```
  Then assert `publicActive().map { it.id }` as an ordered `List` with distinct `createdAt` values.

**S4. Nothing on `publicActive()` warns that it returns full tickets, including personal data.**
- The name reads like "safe for anonymous users". It only filters *which* tickets are returned, not *which fields*: every `Ticket` still carries `creatorId`, `followerIds`, `messages` and the exact `location`.
- Phase 1 has no repository, so D-003's ViewModel will feed this list straight to the anonymous screens. CLAUDE.md's non-negotiable rule and decision 0003 ("must be filtered in the repository layer") depend on the caller knowing this.
- Fix: extend the KDoc in `TicketsList.kt`:
  ```kotlin
  /**
   * Public active tickets for anonymous users (brief I.1.B), newest first.
   * Filters which tickets, not which fields: each Ticket still has creatorId, followerIds,
   * messages and the exact location. Callers serving anonymous users must drop those and
   * coarsen the location (decision 0003).
   */
  ```

## Nit

**N1. Haversine rounding guard.** For near-antipodal points, rounding can push `a` slightly above 1. Then `sqrt(1 - a)` is NaN, and the function returns NaN. This is harmless here: the distances are 10 m to 10 km, and `NaN <= 10.0` is false. Still, it is a one-word fix that is easy to defend: `val a = (...).coerceIn(0.0, 1.0)`.

**N2. The negative-duration skip rule is untested.** `stats()` skips `end - createdAt < 0`, but no test covers it. Fix: add to `stats_on_mixed_list` a RESOLVED ticket with `createdAt = 20 * hour` and a RESOLVED entry at `19 * hour`. Expect `totalTickets` 6, `activeTickets` 2 and `resolvedTickets` 2, with the average unchanged at 4 h.

**N3. Rename `rawList_is_a_copy` to `rawList_snapshot_does_not_change_after_add`.** This only makes sense if S3's mutation test is added. The current name promises more than the test checks.

**N4. The comparator is per instance.** `newestFirst` is created once per `TicketsList`. Moving it to the `companion object` (the plan allowed either) makes "one shared order" explicit. This is cosmetic.

## Coordination notes
- **Francisco, F-004 (dashboard):**
  - Francisco has still not been sent the `DashboardStats` fields: the push after slice 2 was skipped (state.md Log). Send him the Interfaces block from `01-coding-plan.md` before `/push-task`.
  - Points for him: `activeRatio` is in 0..1, so format it with `NumberFormat.getPercentInstance()`. `averageRepairTimeMillis == null` means "no repairs yet" and needs a placeholder string in both `values/` and `values-pt/`. "Active" excludes RESOLVED, so the label should read like "not yet repaired". If S1 is applied, `resolvedTickets` counts only non-active tickets.
- **Francisco, F-002/F-003 (lists, map):** reuse `Ticket.isActive`; do not write a second "not closed" check. Lists key on `id`; `add` rejects blank and duplicate ids.
- **Diogo, D-003 (`TicketsViewModel`, sample data):**
  - Sample tickets need distinct non-blank ids, `scope = PUBLIC` where they should appear anonymously (the default is PRIVATE, decision 0003), `createdAt > 0`, and `currentStatus` equal to the last history entry (decision 0004).
  - Before appending a follower from `findNearby`, check `creatorId`/`followerIds`.
  - New tickets need generated ids (a UUID), or `add` returns `false`.
  - The anonymous UiState must not hold `creatorId`, `followerIds`, `messages` or exact coordinates (S4). A small public projection in `viewmodel/` or `model/` is the simplest way.
- **Firebase phase:** Q9 means anonymous users need an aggregate source for the stats (a stats document or `count()` that the rules allow). The grouping query should pre-filter by category, situation and a bounding box.
- **Docs phase / person:**
  - CLAUDE.md and kotlin-conventions still say "no automated tests". They are now inaccurate (Gate 1 Q1), and the person edits them.
  - `git config core.hooksPath` is unset in this clone, so the pre-commit check did not run. Run `git config core.hooksPath .githooks` once.
