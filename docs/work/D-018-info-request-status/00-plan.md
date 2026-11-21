# D-018: Information requests: right operator button and answered state
Type: bug · Task file: docs/tasks/phase-3/D-018-info-request-status.md
Labels: none

## Problem
Francisco's test, item 7: after an information request on an IN_PROGRESS ticket the operator sees "Start work", and no screen says whether the request was answered.

## Reproduction
1. Operator: open an OPEN ticket, "Start work" -> IN_PROGRESS.
2. Operator: "Request information" (or the citizen: "Ask for clarification") -> INFO_REQUESTED.
3. The operator's only action is "Start work".
4. The other side answers in the thread: nothing on screen changes.

## Root cause
- `operatorTargets(INFO_REQUESTED) = {IN_PROGRESS}` (model and firestore.rules) is correct, but `ActionsRow` always labels IN_PROGRESS "Start work"; it never looks at the status before the request.
- The request lives only as the last `StatusHistory` entry (justification = the question) plus a message. No code compares it with later messages.

## Outcome
- The button and its dialog say "Resume work" / "Resume work?" when the status before the request was IN_PROGRESS; "Start work" otherwise (request made from OPEN). Same transition underneath (INFO_REQUESTED -> IN_PROGRESS).
- While the ticket is INFO_REQUESTED, both detail screens (operator, and participant citizen) show an "Information request" card at the top of the actions slot: the question, "Asked by the municipality" / "Asked by the citizen", and "Waiting for a reply" or "Answered".

## Acceptance checks
- [ ] IN_PROGRESS -> operator requests info -> operator button "Resume work", dialog "Resume work?"; confirm -> IN_PROGRESS.
- [ ] OPEN -> request -> button still "Start work".
- [ ] Citizen asks for clarification on an IN_PROGRESS ticket -> operator sees "Resume work".
- [ ] Card shows the question and who asked; "Waiting for a reply" until the other side sends a message, then "Answered" (live).
- [ ] A message from the side that asked does not count as an answer.
- [ ] After the move back to IN_PROGRESS the card disappears.
- [ ] Anonymous detail: unchanged (no card; anonymous users have no history or messages).
- [ ] English and Portuguese; portrait and landscape.

## Approach
Pure rule in `model/` (new file `model/InfoRequest.kt`):
```kotlin
data class InfoRequest(val question: String, val askedAt: Long, val byMunicipality: Boolean, val statusBefore: TicketStatus)
fun Ticket.pendingInfoRequest(): InfoRequest?          // null unless currentStatus == INFO_REQUESTED
fun InfoRequest.isAnswered(messages: List<Message>): Boolean
```
- `pendingInfoRequest`: last history entry (INFO_REQUESTED); `byMunicipality = !isParticipant(entry.responsibleId)`; `statusBefore` = status of the entry before it (OPEN when there is none).
- `isAnswered`: some message with `sentAt > askedAt` and `byOperator == !byMunicipality` (from the other side). The question's own message has `sentAt == askedAt` (same batch), so it never counts.
- UI: new stateless `ui/components/InfoRequestCard.kt` (SectionCard look, status chip text). `OperatorTicketContent` and `TicketDetailContent` show it first in the slot. `ActionsRow` and `OperatorTextDialog` get `resume: Boolean`.

Alternatives considered: an `answered` field on the ticket (needs a rules change, rejected with option B); a "reply" button tied to the request (more UI, same result).

## Architecture impact
One pure model file, one component, small edits to two screens, strings. No repository, Firestore, rules or notification change.

## Failure modes
- Device clocks: `sentAt` comes from the sender's clock. An answer from a device whose clock is behind the asker's could look older than the question and stay "Waiting for a reply". Accepted (documented); the transition still works.
- A ticket with several requests in a row: only the last one (current status) counts.

## Risks and open questions
None open; option A was chosen (no rules change).

## Out of scope
- Returning to OPEN (option B).
- Notification texts.
- Bug 6 (left as it is, by decision).

## Slices
1. `model/InfoRequest.kt` + `InfoRequestCard` + both screens + strings -> `fix(D-018): show information request status and resume work label`.
