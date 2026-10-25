# D-001: Domain classes and enums

Owner: Diogo (`dev/Diogo`) · Notion plan ID: D1 · Layer: `model` · Phase 1 · Status: to do
Depends on: none

## Goal
Create the domain model from the Option A diagram, in English, all with default values so they are easy to instantiate in previews and tests. **Do this first and push `dev/Diogo`: every task of Francisco depends on it.**

## What to build
Enums `TicketStatus` (OPEN, INFO_REQUESTED, IN_PROGRESS, RESOLVED, CLOSED), `Situation` (DAMAGE, BREAKDOWN, DESTRUCTION, THEFT), `Scope` (PUBLIC, PRIVATE), `AttachmentType`, `UploadState`, `RegistrationStatus`; `Coordinates` (one type shared by Ticket, Citizen and Municipality); `User` (base: name, nif, email, phone, photo?), `Citizen` (georeferencing, registrationStatus, impactDetection), `Operator`; `Municipality` (name, emailDomain, center, radiusKm); `Category` (name, active); `Ticket` (description, situation, scope, location, currentStatus, createdAt, category, creator, followers, attachments, messages, statusHistory); `Message`; `StatusHistory` (status, dateTime, justification?, responsible); `Attachment` (type, file, thumbnail?, uploadState, uploadedBy).

## Files
`model/*.kt`

## Acceptance checks
- [ ] Every class compiles and can be built with no arguments.
- [ ] `User` is the base (interface or open class); `Citizen` and `Operator` implement it as data classes (no single `User` with a `role`).
- [ ] `Ticket` follows Option A: one ticket with a list of following citizens; there is no `Group` class.
- [ ] `currentStatus` is kept on `Ticket` on purpose (list and filters do not read the history); note the justification for the report.

## References
- Brief: `docs/brief/AMov.ProjetoKotlin_PT.pdf`; domain model: `docs/architecture/overview.md` (Option A, decision 0002).
- Plan and order: [phase 1 index](README.md).
