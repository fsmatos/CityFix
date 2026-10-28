# 0004: Keep `Ticket.currentStatus` next to the status history

Status: accepted (D-001, 2026-10-27)

## Context
The diagram gives `Ticket` both `estadoAtual` and a composed `HistoricoEstado` (1..*), so the current status can be derived from the last history entry. The brief asks for full traceability of every transition with timestamps, which the history provides. But most reads only need the current status:
- the ticket list and the anonymous "public, active" list;
- the dashboard (% active tickets, average repair time);
- filters by status.

In Firestore the history will likely become an array or a subcollection, and a query cannot filter on "the last element" of either. `whereEqualTo("currentStatus", ...)` needs a top-level field.

## Decision
Keep `Ticket.currentStatus: TicketStatus` (default `OPEN`) as a denormalised copy of `statusHistory.last().status`. The history stays the audit trail; `currentStatus` is the read model for lists, the dashboard and queries.

Rejected: deriving the status from the history on every read (each list draw scans every ticket's history; impossible to query in Firestore).

## Consequences
- The two values must never change separately. Every transition appends a `StatusHistory` entry (status, `dateTime`, `responsibleId`, `justification` when reopening or requesting information) and sets `currentStatus` in the same operation: one pure transition function in `model/` (D-002 or later), and one batched write or transaction in `data/` in the Firebase phase.
- One extra field per write, in exchange for reading a single field in every list and query.
- A default `Ticket()` has `currentStatus = OPEN` with an empty history. Sample data must include at least one history entry whose last status equals `currentStatus`.
- Code that needs a specific transition time (RESOLVED for the 5-day reopen window, the 24 h warning and the average repair time) reads it from the history with `lastOrNull`/`firstOrNull { it.status == RESOLVED }`, never from `currentStatus`, and skips tickets without that entry.
