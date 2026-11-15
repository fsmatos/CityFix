# F-014: Citizen: my tickets, message thread and reopening
Branch: dev/Francisco · Base: 206b5f7
Remote: origin/dev/Francisco

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-14 | feature, task file `docs/tasks/phase-2/F-014-citizen-tickets-messages-reopen.md` |
| Plan | done | 2026-11-15 | Draft 2026-11-14; revised 2026-11-15 for D-009 (session user, My tickets entry settled, real test accounts); 7 slices; Q1-Q14 with defaults |
| Gate 1: plan approved | done | 2026-11-15 | approved with the person's Q1-Q15 answers; Q1 follows brief II.2, Diogo agrees |
| Code | done | 2026-11-15 | Q15 renumber + 7 slices (`cac6213`..`6ffb359`); lint 0 errors, 22 warnings; device checks D1-D15 not run |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.
- 2026-11-14: fetched origin; origin/main had 0 commits missing from dev/Francisco; the person chose not to merge (already up to date). Started F-014 from the task file. D-009 (login) is not done yet (origin/dev/Diogo has nothing after D-008), so F-014 keeps the F-011 `currentUser` stand-in.
- 2026-11-14: Plan written (planner).
  - Read first: the task file, the brief (I.2.E, II.2, II.4, II.5, checked in the brief text), decisions 0007/0010/0011/0012/0013/0014, the overview handovers, the repository/mapping/ViewModel/ShowScreen/navigation/MainActivity code, the Contacts_#3 and T8 demos, and class slides 04/07/08/09 with P06-P10.
  - Approach:
    - messages subcollection with a live listener and queued `document().set()`;
    - one private `commitStatusChange` batch for reopen and close-on-read (ticket status fields, `publicTickets` mirror, `ticketLocations.active = isActive`, `statsIncrements`);
    - `detailAudience(user, ticket)` with the REGISTERED slot in `ShowScreen`;
    - a per-destination `TicketDetailViewModel`, created only for participants and operators;
    - a stateless `MessageThread` for D-013;
    - `MyTicketsScreen` behind a citizen-only top-bar action.
  - 7 slices.
  - Open questions Q1-Q12 for Gate 1. Q1: brief II.2 lists citizen clarification requests under "Pedido de Informações"; the plan recommends keeping D-008 Q1 (plain message).
  - Diogo's files touched if the defaults pass: `Destinations.kt`, `MainTopAppBar.kt`, `MainActivity.kt`, `Message.kt` (Q4), `TicketLifecycle.kt` (Q5). Tell him before slices 1, 3 and 7.
  - Device checks D1-D14 use stand-in users A/B/C/N and console-edited RESOLVED entries for day 4 / day 6.
- 2026-11-15: Before Gate 1 the person asked to bring in the latest changes. origin/main had D-009 (login, MFA, role navigation, session wiring for tickets, reports and the 10 km gate; 31 commits). Merged origin/main into dev/Francisco with no conflicts (assembleDebug and lint pass, 0 errors, 23 warnings). The draft plan (written before D-009 reached main, assuming the `currentUser` stand-in) must be checked against D-009's session before Gate 1. D-010 (registration) is on origin/dev/Diogo but not yet in main.
- 2026-11-15: Plan revised for D-009 (planner), before Gate 1. Read: D-009 state/00-plan/review, decision 0015, the overview's "Auth and session (D-009)", "Handover (D-009)" and "Open: lifecycle fields", and the code (`Session.kt`, `RoleAccess.kt`, `Destinations.kt`, `MainTopAppBar.kt`, `PlaceholderScreen.kt`, `MainActivity.kt`, `CityFixApplication.kt`, `TicketsViewModel.kt`, `FirestoreUsersRepository.kt`, `TicketLifecycle.kt`, `SampleTickets.kt`, `DebugSeed.kt`). What changed:
  - User source: every `app.currentUser` is now `session.ticketsUser` in `CityFixApp` (active citizen or operator, else null). Pending citizens therefore get PUBLIC and no thread (decision 0015 Q2, D-008 Q11). `detailAudience(user: User?, ticket)` keeps its signature (no `Session` in `ShowScreen`). New point 0 in the Approach; the detail ViewModel takes the user in its constructor (as `ReportViewModelFactory`), safe because D-009's redirect clears the back stack on any role change.
  - Q2 settled by D-009: `MyTicketsRoute`, the "My tickets" overflow menu entry (`menuItemsFor(CITIZEN)`), `RequireAccess(CITIZEN)`, `TopBarScreen.MY_TICKETS`, `toTopBarScreen()` and the title/menu strings already exist. Dropped from the plan: the person-icon top-bar action, `showMyTickets`/`onMyTicketsClick`, every `MainTopAppBar.kt` and `Destinations.kt` code change, and the strings `top_bar_title_my_tickets` and `cd_my_tickets`. Slice 7 now only adds `MyTicketsScreen` + `TicketCard.status`, replaces the placeholder inside the existing guard, and updates two "Placeholder until F-014" comments (Diogo's `Destinations.kt`, `PlaceholderScreen.kt`).
  - Q9 partly settled: operators cannot reach the list or detail today (home = panel, menu without a list), so the OPERATOR mapping only prepares D-013; recommendation unchanged. Q6 wording: participants (operators once D-013 routes them).
  - Q7 now also closes point 1 of the overview's "Open: lifecycle fields, F-010 vs D-008" (agree with Diogo).
  - Q11: 0015 is D-009's; recommend 0016 unless D-010 took it.
  - New Q13 (expand toggle on My tickets; D-009 handover): default no. New Q14 (test accounts): default reuse Diogo's three console accounts plus one new active citizen.
  - Slices 3 and 6: the `MainActivity` detail call uses `session.ticketsUser`; slice 3 also rewords D-009's Q7 and detail-audience comments. "Do not touch" now lists D-009's session, guards, redirect, gate, `logOut`, `RoleAccess.kt`, `MainTopAppBar.kt`; the `currentUser` rule is gone.
  - Device checks: the stand-in rebuild (A/B/C/N by editing `currentUser`) is replaced by real accounts A (active), C (second active), P (pending), O (operator, used only through REST because the operator UI is D-013). Preconditions list Francisco's SHA-1/SHA-256 (D-009 added only Diogo's), a Google Play AVD, how to create C (Identity Platform test number, Auth user, `users/{uid}` fields), and test data made through the app (T1 by A, C joins it with the same category + situation, T2/T3 for day 4/day 6) plus `sample-3` edited (`creatorId` and `participantIds` = A's UID). New D1b (pending) and D15 (log out on the participant detail).
  - New failure mode (log out / involuntary sign-out on the detail) and risk (D-010 may also edit `MainActivity.kt`; Diogo's files reduced to `MainActivity.kt`, `Message.kt`, `TicketLifecycle.kt` plus two comment lines).
  - Docs plan: overview sections touched by D-009 (App shell, Navigation table, Handover D-009/F-011, Open lifecycle fields), 0013 gets D-009's suggested amendment line, 0010 line 73 fix, a suggested line for Diogo's 0015 (not edited).
  - Q1 left open for the person, with the I.3.E note from D-009's brief paste.
- 2026-11-15: The plans were shortened at the person's request, with the content unchanged. Q15 was added (decision number; it replaces Q11).
- 2026-11-15: Second shortening pass; 00-plan is now a one-page summary. Detail from 00-plan (user source, message/batch rules, audience notes, rejected alternatives, Q reasons and alternatives, failure modes, out of scope) moved to 01-coding-plan "Decisions" and "Failure modes"; the strings table became a key list; the D6 REST payload is one line. No decision, signature, file, risk, open question or device check removed.
- 2026-11-15: The person answered Q1-Q15 after the questions were re-explained against the brief. Q1 follows brief II.2 (a citizen's clarification request sets INFO_REQUESTED; needs Diogo's agreement because it adds a citizen move to his `TicketLifecycle.kt` and pauses D-014's geofence); Q2 keep D-009; Q3 queued (brief IV covers offline writes for messages); Q4-Q6 defaults; Q7 the RESOLVED history entry; Q8-Q10 as the brief says (exact map for participants, operator audience prepared, full history with justifications and automatic close); Q12-Q15 defaults (F-013's record renamed to 0017, F-014 uses 0018). 00-plan updated (approach, slice 6, risks, answered questions). Gate 1 approval still to be given explicitly.
- 2026-11-15: Gate 1: approved by the person ("Approve, Diogo agrees"): all 7 slices, including the Q1 change to Diogo's `TicketLifecycle.kt` (citizen "Ask for clarification" -> INFO_REQUESTED), which the person confirms Diogo agrees to.
- 2026-11-15: Code (implementer). Followed 01-coding-plan with the Gate 1 answers in 00-plan overriding it (Q1: the citizen's clarification request sets INFO_REQUESTED). Verify (assembleDebug + lint) passed before every commit: 0 errors, 22 warnings each time (the 23 recorded at the D-009 merge is now 22; no new warning ids).
  - `cac6213` docs: Q15, F-013's record renamed to `0017-media-preparation-and-uploads.md` (title, status line notes the renumbering); links fixed in the overview (3), decisions 0006/0010/0013 and `docs/report/notes.md`. The other "decision 0015" mentions in the overview are D-009's record and were left. Historical `docs/work` logs untouched.
  - `71fc640` slice 1: `Message.byOperator`, `MESSAGES`, `messageDocument`, `toMessage`, `messagesFor`, `sendMessage`.
  - `f99858a` slice 2: `toStatusFields`, `reopenTicket`, `closeExpiredTicket`, private `commitStatusChange`.
  - `de470b4` slice 3: public `isParticipant`, `detailAudience`, `ShowScreen(now, participantContent)`, `ExactLocationSection`, history labels; `MainActivity` detail audience and the Q7 comment; `TicketMap` KDoc now says exact = participants and operators.
  - `e174725` slice 4: `MessageThread`.
  - `837f1f5` slice 5: `TicketDetailUiState`, `TicketDetailViewModel`, factory.
  - `c4e2e75` slice 6: `TicketDetailScreen` / `TicketDetailContent`, dialog, close-on-read, `MainActivity` wiring, and "Ask for clarification" (Q1).
  - `6ffb359` slice 7: `MyTicketsScreen`, `TicketCard.status`, placeholder replaced, Diogo's two KDoc lines.
  - Deviations from 01-coding-plan:
    - Q1 (Gate 1 answer, not the plan's "plain message"): `TicketLifecycle.allowedTransitions` (Diogo's file, he agrees) now lets an ACTIVE participant citizen move OPEN or IN_PROGRESS to INFO_REQUESTED; `transition` already requires the justification (the question). KDoc updated. New `TicketsRepository.askForClarification(ticket, by, question)`; `commitStatusChange` takes an optional message map so the message and the status change go in one batch with the publicTickets mirror, `ticketLocations.active` and `statsIncrements`. The button shows only for a `Citizen` when INFO_REQUESTED is allowed. The answer moving it on is D-013's (operator targets: INFO_REQUESTED -> IN_PROGRESS only).
    - Reopen and clarification share one dialog: the UiState has `openDialog: DetailDialog?` (REOPEN, ASK_CLARIFICATION), `dialogText`, `dialogError` instead of `isReopenDialogOpen`, `justification`, `reopenError`; ViewModel functions `onReopenClick`, `onAskClarificationClick`, `onDialogTextChange`, `onDialogDismiss`, `confirmDialog(ticket)` instead of `onJustificationChange`, `onReopenDismiss`, `confirmReopen`. `TicketDetailContent` parameters follow these names.
    - Strings: `reopen_cancel` is `dialog_cancel` (shared); new `clarification_button`, `clarification_dialog_title`, `clarification_dialog_text`, `clarification_question_label`, `clarification_confirm`, `clarification_error_blank`, `clarification_error_not_allowed` (EN + PT).
    - Message bubbles are capped at 320 dp (`widthIn(max)`) instead of 85 % of the width (simpler).
    - `TicketDetailContent` puts `imePadding()` on ShowScreen so the input stays above the keyboard.
    - The "Placeholders" comment in `MainActivity` moved below the My tickets destination (it no longer is one).
  - Device checks D1-D15: not run (they need the test accounts A/C/P/O that the person must prepare, Francisco's SHA keys in Firebase and a Google Play AVD).
