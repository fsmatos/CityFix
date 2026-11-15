# 0007: Ticket detail by audience

Status: accepted (F-005, Gate 1 on 2026-11-01; Q2 revised at review on 2026-11-01). Amends [decision 0006](0006-anonymous-projection-phase-1.md): the anonymous projection should also drop `description` and `statusHistory` (handover to Diogo, not yet done).

Amended by F-014 (2026-11-15, see [decision 0018](0018-citizen-tickets-messages-reopen.md)): `detailAudience(session.ticketsUser, ticket)` picks the audience; REGISTERED gets the exact map, every history entry with justifications and "Closed automatically", and the thread through a content slot; OPERATOR is prepared for D-013; `ShowScreen` takes `now`. S4 and the list/detail consistency were done by D-007 (see the amended 0006).

## Context
The brief defines three different ticket details, not one detail with optional extras:
- **I.1.C, anonymous citizen:** "Detalhe de uma ocorrência pública: ocultando dados pessoais do autor e georreferenciação, apenas com imagem(ns) e/ou vídeo(s) e mapa com zoom afastado."
- **I.2.D, registered citizen:** "acesso às funcionalidades do utilizador anónimo", so for a public ticket that is not theirs a registered citizen gets the I.1.C view.
- **I.2.E, registered citizen, own tickets:** "Detalhe de uma ocorrência, com acesso a troca de mensagens", on "ocorrências (do utilizador)", the tickets the citizen created or follows.
- **I.3.E, operator:** "visualização integral ... (com coordenadas, contactos do autor e histórico completo com fotos e vídeos)" and "transição de estados (com timestamp)".
- For reference, **I.1.B** (anonymous list) shows "categoria, situação e thumbnail".

Phase 1 has no login, so every real user is anonymous and `TicketsViewModel.tickets` gives only projected tickets (decision 0006). The non-anonymous views can be shown only in previews until the Auth/Firebase phase.

Revision 1 of the F-005 plan showed the description and the status history to every viewer. Gate 1 rejected it: the person requires the project to follow the brief strictly, and I.1.C says "apenas".

Decision 0006's projection (`toAnonymousView()`) blanks the author fields and rounds the location, but keeps `description` and `statusHistory`, which I.1.C now hides.

## Decision
- **An audience enum passed by the caller.** `enum class DetailAudience { PUBLIC, REGISTERED, OPERATOR }` and a required parameter: `ShowScreen(ticket: Ticket?, audience: DetailAudience, modifier: Modifier = Modifier)`. No default, so each call site states who is looking. The screen branches with an exhaustive `when (audience)`, so a later task that adds messages (I.2.E) or coordinates and contacts (I.3.E) does not compile until it decides for every audience.
- **Who is which audience** (decided by the caller, never by the screen):
  - `PUBLIC`: anonymous users (I.1.C), and any registered citizen viewing a public ticket they did not create and do not follow (I.2.D);
  - `REGISTERED`: the registered citizen who created or follows this ticket (I.2.E);
  - `OPERATOR`: any municipal operator (I.3.E).
- **What each audience sees in phase 1:**
  - every audience: header (category, situation, status badge), photo/video tiles, approximate location;
  - `REGISTERED` and `OPERATOR` also: "Reported on <date, time>", the description, and the status history in stored order with a locale-aware date and time. They render the same thing until messages, coordinates and contacts exist.
  - No coordinates as text for anyone in phase 1.
- **PUBLIC header (Gate 1 Q2, revised at review N2).** "apenas com imagem(ns) e/ou vídeo(s) e mapa" read literally leaves an unlabelled map and photos. Gate 1 first chose I.1.B's fields only (category + situation). At review the person added the status badge, so the detail matches the public list card, which already shows it. The detail therefore reveals nothing the list does not already show for that ticket.
- **The enum lives in `ui/screens/ShowScreen.kt`.** Only this screen and D-006's call use it. It moves to `model/` only if the Auth task needs it there.
- **Media in phase 1:** type-icon tiles with "Not available yet", never a fake photo, and no Coil. Phase 1 has no real file to load.
- **Map: a static placeholder (Gate 1 Q7).** `ui/components/CoarseLocationMap` draws a box with a location icon and the caption "Approximate area (about 1 km)", or "Location not available" for (0, 0). No map library, no API key, no manifest change. Reason: the person chose not to add a dependency, a permission or a key in this task; CLAUDE.md requires asking before touching keys, and osmdroid (the no-key option) is archived. The map is behind one component, so the real map replaces one file.

### Rejected
- **`isPublicView: Boolean`.** It cannot express three views: registered citizens get messages, operators also get coordinates and contacts.
- **Reading the role inside the screen** (from the ViewModel, Auth or a `CompositionLocal`). The screen would stop being stateless and previewable with plain data, and it would need the creator/follower relation, which is data, not a role.
- **`DetailAudience` in `model/`.** It is a presentation choice; `model/` holds entities and pure rules. Kept as an option for the Auth task.
- **A default `audience = PUBLIC`.** Safe, but it hides the decision at the call site, and `modifier` would no longer be the first optional parameter.
- **Description and history for anonymous users** (revision 1 of the plan): against I.1.C's "apenas".
- **Coordinate text, a pin on the rounded point, an interactive map:** "ocultando ... georreferenciação"; a pin suggests a precise spot; zoom-in defeats "zoom afastado" and fights the page scroll.
- **A sample photo shown as the ticket's photo:** dishonest.
- **Projecting in the screen:** decision 0006, the rule must hold below the UI.

## Consequences
- **Projection change handed over to Diogo (review S4).** `toAnonymousView()` should also blank `description` and clear `statusHistory` (keeping `currentStatus`), and decision 0006 should record it. Until then those two fields are hidden from anonymous users **only in the UI**, against the CLAUDE.md rule "enforce in the repository layer, not only in the UI". `ShowScreen` is correct before and after the change, because PUBLIC never reads them. Only Diogo can change `model/TicketPrivacy.kt` and his decision record, so 0006 is not edited here.
- **REGISTERED needs the creator/follower check.** The Auth/ViewModel layer must map the role *and* the relation to the ticket: a logged-in citizen is `REGISTERED` only on a ticket whose `creatorId` is theirs or whose `followerIds` contains them; otherwise `PUBLIC`. Mapping "logged in as a citizen" straight to `REGISTERED` would show another author's free-text description, which may contain personal data. The non-anonymous source must also be unprojected (decision 0006 consequences).
- **Known gap against I.1.C: no real map.** "mapa com zoom afastado" is not met; the screen shows a placeholder. Accepted knowingly at Gate 1. To close it, replace the body of `CoarseLocationMap`:
  - osmdroid 6.1.x: no key or account; `INTERNET` + `ACCESS_NETWORK_STATE`; `AndroidView { MapView }` with `onDetach()` in `onRelease`; User-Agent and OSM attribution (tile policy); archived library;
  - or Google Maps Compose: native composable; needs a Maps API key the person creates and keeps out of git;
  - either way: fixed zoom about 14, gestures off, a ~600 m circle instead of a pin, a placeholder in previews through `LocalInspectionMode`. Verify on an emulator with network.
- **List/detail consistency.** The status badge is shown to anonymous users in both the list card and the detail (review N2). But the expanded list card (`TicketCard(isExpanded = true)`, F-003 Gate 1 Q2) shows the description to anonymous users, while the detail hides it. The two are now **inconsistent**: the same anonymous user can read in the list the text the detail withholds. Decide it together with Diogo's projection change: once `toAnonymousView()` blanks `description`, the expanded anonymous card has nothing to show (and brief I.1.B agrees), so the expand toggle should be hidden or forced off for anonymous users (the D-005/D-006 handover in the overview).
- A ticket that leaves the public list (for example becomes RESOLVED) switches the anonymous detail to "Ticket not found".
- The task check "status history shown in order with timestamps" applies to REGISTERED/OPERATOR and can be seen only in previews in phase 1.
