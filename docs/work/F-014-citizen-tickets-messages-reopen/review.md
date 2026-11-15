# F-014 review

Range `fbbbbe0..HEAD` (8 commits). Reviewed 2026-11-15 against 00-plan (Q1-Q15), 01-coding-plan, state.md deviations, brief I.1.C, I.2.E, II.2, II.4, II.5, IV and Contacts_#3.

**Verdict: approve, with 1 should-fix for the docs phase.** 0 blocking, 1 should-fix, 1 nit. `./gradlew assembleDebug lint`: BUILD SUCCESSFUL, 0 errors, 22 warnings (the same ids as before, none in F-014 files).

What holds:
- **Status batch:** reopen, close-on-read and the clarification request each write one batch: ticket status and history (through D-008 `transition` / `closeIfExpired`), the `publicTickets` mirror (PUBLIC only), `ticketLocations.active` and `statsIncrements`. The figures add up: a reopen is active +1, resolved -1 and minus the old repair time; a close and OPEN/IN_PROGRESS -> INFO_REQUESTED add zeros.
- **Lifecycle change:** the new citizen move in `TicketLifecycle.kt` is minimal and correct. Only an ACTIVE participant gets it, only from OPEN or IN_PROGRESS, and the existing justification check already rejects a blank question. The reopen branch is unchanged.
- **Privacy:** `ticketsUser` is null for anonymous, pending and loading sessions, so they get PUBLIC. A citizen who is not a participant also gets PUBLIC (a projected ticket has a blank creator). `TicketDetailViewModel` is only created in the non-PUBLIC branch, so `messagesFor` is never called for these users.
- **Messages and My tickets:** `byOperator` round-trips through `messageDocument`/`toMessage`. `MessageThread` is stateless and takes no role, so D-013 can reuse it. My tickets filters the citizen's own `participantTickets` (projected public copies have a blank creator and are left out).
- **Strings:** 31 new keys in EN, the same 31 in `values-pt`.
- **Decision rename 0015 -> 0017:** no link to `0015-media` is left outside `docs/work` logs, and Diogo's `0015-session-role-navigation-mfa.md` is untouched.
- **Class patterns:** the per-destination factory and `AlertDialog` follow Contacts_#3. The slot and `combine` carry "beyond class" KDoc.

## Blocking
None.

## Should-fix
- **S1. Decision 0011 now contradicts the code (Q1).** `docs/decisions/0011-ticket-lifecycle-rules.md` lines 20-30 still show "-" for the citizen on OPEN and IN_PROGRESS. Its Q1 says a citizen's question "does not set INFO_REQUESTED" and lists citizen -> INFO_REQUESTED as rejected. `TicketLifecycle.allowedTransitions` now allows it, and at the defence the record would contradict the code. 03-docs-plan only plans "II.2 Context line only with permission", which was written before Gate 1. Fix in the docs phase: since Diogo agreed, amend 0011's table (citizen column, OPEN and IN_PROGRESS rows: "INFO_REQUESTED (clarification, F-014)"). Mark its Q1 "Superseded by F-014 Gate 1 Q1 (brief II.2), see 0018". Change the 03-docs-plan bullet to match.

## Nit
- **N1. PT "Você" for the own message label** (`values-pt/strings.xml`, `message_author_you`). In European Portuguese "Você" sounds abrupt in a UI. Use "Eu" (or "Enviada por si").

## Open
- Device checks D1-D15 not run: test accounts A/C/P/O, Francisco's SHA-1/SHA-256 in Firebase and a Google Play AVD are still pending.
- D-013 handover: the only operator move out of INFO_REQUESTED is IN_PROGRESS. If a citizen asks while the ticket is OPEN, answering it marks the ticket "In progress". D-013 should decide whether to accept this or add INFO_REQUESTED -> OPEN.
- D-014: a citizen clarification also pauses the geofence (IN_PROGRESS -> INFO_REQUESTED); this risk is already in the plan, so tell Diogo.
- F-016: brief II.2 says the other party is notified; that is F-016's job.
- state.md header says `Base: 206b5f7` and this review used `fbbbbe0`, the plan commit. The code range is the same.
