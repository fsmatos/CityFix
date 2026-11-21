# D-018: Information requests: right operator button and answered state

Type: bug · Reported by Francisco during testing (2026-11-21), item 7. Option A chosen by Diogo (no rules change).

## Problem
When the municipality or the citizen asks for information on a ticket that was already
"In progress", the operator's only button is "Start work" again. Nothing shows whether the
request has been answered.

## Brief
II.2: either side may ask for information; the other side is notified and answers.

## Acceptance
- Request made while IN_PROGRESS: the operator's button says "Resume work".
- The detail (operator and participant) shows the pending question, who asked, and "Waiting for a reply" or "Answered".
- No change to firestore.rules.
