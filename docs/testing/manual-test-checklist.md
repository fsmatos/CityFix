# CityFix: manual test checklist (brief coverage)

Tested on: `dev/Francisco` @ `04c7b48` (includes `origin/main` @ `4ee55bb`; F-019 removed FCM, notifications are local only). Tick each box on a device or emulator.

**Setup:** 2 devices/emulators (one citizen, one operator), console test phone numbers for SMS, emulator location via Extended controls or `adb emu geo fix <lon> <lat>`. Accounts: anonymous, citizen C (active), citizen P (pending), operator O.

## 0. Build and start
- [x] `./gradlew assembleDebug` passes; `./gradlew lint` has 0 errors.
- [x] Clean install starts; location permission asked at start.
- [x] Deny location: message with "Try again" and "Open settings" works.

## I.1 Anonymous citizen (10 km of Pr. Republica)
- [x] Location inside 10 km (e.g. 40.2100, -8.4190): app opens.
- [x] Location outside 10 km (e.g. Lisbon 38.72, -9.14): blocked screen.
- [x] Location off / no fix: correct message, no crash.
- [x] A. Dashboard: average repair time, total tickets, % active; numbers change after a resolve.
- [x] B. List: only PUBLIC and active tickets; category, situation and thumbnail shown.
- [x] B. A PRIVATE ticket never appears for anonymous users.
- [x] C. Detail: photos/videos shown; no author name/e-mail/phone; no coordinates as text.
- [x] C. Map is zoomed out (circle, no marker) and cannot be zoomed or moved.
- [x] No report button and no "My tickets" for anonymous users.

## I.2 Registered citizen
**A. Registration**
- [x] Register citizen: name, e-mail, phone, NIF, location, optional photo, impact-detection switch.
- [x] Invalid e-mail, phone (not 9 digits / not 2 or 9), NIF (bad check digit), weak password: each rejected with a message.
- [x] Same citizen NIF twice: refused ("NIF taken"); the Auth user is not left behind.
- [x] New citizen lands on "waiting for validation"; can see the dashboard only (no report).
- [x] After operator activation, the citizen switches live to the full citizen home.

**B. Authentication**
- [x] E-mail verification link, then SMS code at account setup, then signed in.
- [x] Log in with e-mail/password (new accounts: no SMS at login, decision 0026; old accounts: SMS step at login).
- [x] Wrong password / unknown e-mail: clear message.
- [x] Forgot password sends the reset e-mail; reset works.
- [x] Close and reopen the app: still signed in (session cached). Log out works.

**C. Profile editing**
- [x] Phone, photo, e-mail (verification link) and password can be changed.
- [x] Name, NIF and location cannot be changed (read-only).
- [x] Impact-detection switch can be turned on/off.

**D.** - [x] Citizen still has dashboard, public list and public detail.

**E. Tickets**
- [x] Open: category list shows only ACTIVE categories.
- [x] Situations: Damage, Fault, Destruction, Theft.
- [x] All fields required; submit refused without description or without at least one photo/video.
- [x] Attach photo from camera, photo from gallery, video from gallery; limit of 3.
- [x] Scope public/private chosen and respected (private hidden from anonymous list).
- [x] Precise location required; "Approximate" location refused with a message.
- [x] My tickets lists all own tickets with their status.
- [x] Detail: exact map, status history, message thread with the municipality.
- [x] Ask for clarification (sets "Info requested", justification required).
- [x] Reopen a RESOLVED ticket within 5 days (justification required) -> OPEN.
- [x] Reopen not offered after 5 days / on CLOSED tickets.

**Grouping (Obs.)**
- [x] Citizen 2 reports same category + situation within 10 m of an active ticket: joins it (snackbar), no new ticket.
- [x] Same place, different category or > 10 m: new ticket.
- [x] Next to a RESOLVED ticket: new ticket.
- [x] Follower sees the ticket in My tickets and gets its notifications; reporting it again says "already following".

**F. Impact detection**
- [x] Switch on in profile: a hard shake/bump shows a "Report" snackbar (emulator: Virtual sensors > Accelerometer).
- [x] "Report" opens the form prefilled with Damage + Pavement + current location.
- [x] Only one suggestion per 30 s; none while already on the form; none with the switch off.

## I.3 Municipal operator
- [ ] A. Register operator: e-mail outside the council domain refused; operator NIF unique; account active at once (after e-mail + SMS).
- [ ] B. Login, forgot password, session cache (as citizen).
- [ ] C. Profile: name and NIF read-only; others editable.
- [ ] D. Pending citizens list; detail shows name, NIF and location on the map; Activate (with confirm).
- [ ] E. Categories: create, edit name, activate, deactivate; duplicate/blank/over 40 chars refused.
- [ ] E. Deactivated category disappears from the citizen's report form.
- [ ] E. Panel: all tickets (public and private), filters by status/category, order.
- [ ] E. Detail: exact coordinates, author contacts, full history with photos/videos and author names.
- [ ] E. Request information (message required) -> Info requested.
- [ ] E. Start work (optional note) -> In progress, with timestamp in history.
- [ ] E. Resolve requires 1-3 proof photos/videos -> Resolved; proof labelled "Resolution proof".
- [ ] E. CLOSED ticket: no actions and no message input.

**Proximity (Obs. / II.3)**
- [x] Ticket IN_PROGRESS; operator device moves from > 20 m to < 20 m: citizen gets "council team near" notification.
- [ ] At 25 m nothing; second entry within 1 h does not alert again.
- [ ] Ticket not IN_PROGRESS: no alert.

## II. Ticket lifecycle
- [ ] Open -> Info requested -> In progress -> Resolved -> Closed, each with timestamp and author in history.
- [ ] Resolved more than 5 days ago shows as Closed and is locked (test by editing `statusHistory` dateTime in the console or changing the device clock).
- [ ] Closed history entry says "Closed automatically".

## III. Notifications
- [ ] Notification permission asked (Android 13+); denial shows a message.
- [ ] Citizen notified on each status change (app open and app closed: worker runs every 15 min).
- [ ] Citizen notified of operator messages; operator notified of citizen messages/clarifications.
- [ ] Proximity alert (see I.3).
- [ ] 24 h before closing warning (4 days after resolve; test with the clock or console).
- [ ] Tap on a notification opens that ticket. Log out clears the tray.

## IV. Persistence and cloud
- [ ] Airplane mode: list and detail still load from cache.
- [ ] Airplane mode: create a ticket and send a message; they sync when back online.
- [ ] Photos shown as "Upload pending" offline, uploaded by WorkManager when online (check Storage in the console).
- [ ] Uploaded photo is compressed (max 1600 px, JPEG) and has a 256 px thumbnail.

## V. Development notes
- [ ] Rotate on every screen (dashboard, list, detail, report form with draft, register form, operator detail): no data lost, layout usable.
- [ ] Device language Portuguese: every text in PT; English otherwise.
- [ ] Top bar/menu/FAB change with role and screen (anonymous, pending, citizen, operator).
- [ ] TalkBack: buttons, images and switches have labels.
- [ ] Permissions requested at runtime: location, notifications (camera via system intent, no permission needed).
- [ ] Full-screen photo/video viewer and pinch zoom on the ticket map (F-018).

## VI. Delivery
- [ ] Technical report PDF written (F-017 still "to do").
- [ ] ZIP `AMOV2627.2018013532.2018044613.zip` from `scripts/make-delivery-zip.ps1`; no `.gradle`, `build`, `app/build`, `local.properties`.
- [ ] ZIP contains `app/google-services.json`, the Maps key and the PDF.
- [ ] Unzip in a new folder and build/run it once.
- [ ] Uploaded to Nonio before 2026-11-22 23:59.

## Known gaps to explain at the defence (not bugs to fix)
- MFA: SMS verified once at sign-up, not at every login (decision 0026); brief I.2.B/I.3.B say "duplo fator".
- Proximity works only while the operator's app is open; no background location (decision 0022), brief V lists it.
- Push: notifications are local only (listener + worker); FCM removed by F-019 (decision 0027), brief III asks for remote too.
- Impact detection uses total acceleration, not only the vertical axis (decision 0023).
- Firestore/Storage rules drafted but not deployed (test mode until 2026-12-10).
