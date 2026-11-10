# F-010 review
Reviewed: `4e4cbd9..HEAD` on `dev/Francisco` (8b47917 rules + decisions 0010/0011, 48abada build, fac91e4 `CityFixApplication`, plus the state.md commits 0647d20 and 9ed404e). `state.md` names `d9a4d02` as Base; `d9a4d02..4e4cbd9` is only the start and plan commits. Checked against 00-plan (schema table and rules bullets that Gate 1 approved), 01-coding-plan, 03-docs-plan, the Gate 1 answers Q1-Q12 in state.md, the task file `docs/tasks/phase-2/F-010-firebase-setup.md`, the brief (`docs/brief/AMov.ProjetoKotlin_PT.pdf`, read as text with `pdftotext`, sections I-IV), CLAUDE.md and the kotlin-conventions skill. Date: 2026-11-10.

**Verdict: changes required (one blocker, a one-line rules fix).** The build, the Firebase container and the persistent cache match the plan. No secret is committed. The schema in 0010 follows the Gate 1 defaults with one widening (S1). The anonymous side holds: anonymous users can read only `publicTickets`, `categories` and `municipality`, and `publicTickets` keys are whitelisted. But the rules let anyone who types an address in the municipality's domain register as an operator without proving they own it. An operator can read every citizen's NIF, phone, georeferencing and every ticket's exact location (B1). The rules are drafts and nothing is deployed, but D-009 builds registration on them, so fix it before `/push-task`.

## Checks run
- `./gradlew assembleDebug lint`: BUILD SUCCESSFUL. Lint: 0 errors, 17 warnings (AndroidGradlePluginVersion 1, GradleDependency 3, NewerVersionAvailable 5, RedundantLabel 1, UnusedResources 7). That is the 16-warning baseline plus `GradleDependency` for `firebase-bom` 34.19.0 vs 35.1.0, accepted under R2. No warning comes from a file this task touched.
- **Secrets.** `git ls-files app/google-services.json` is empty. `git log --all -- app/google-services.json` is empty, so it was never committed. `git check-ignore -v` matches `.gitignore:20`. Grep of the whole diff for `AIza`, `api_key`, `project_id`, `mobilesdk`: no hits. `firebase.json` has no project id, and there is no `.firebaserc`. I did not open the json.
- **Device.** `adb` is not on PATH in this shell, so the smoke test could not be run here either (see S6).
- **Rules syntax.** Checked by reading, against the rules language reference. `rules_version = '2'`, path interpolation with string `+` (`nifs/$(role + '_' + nif)`), `getAfter`/`existsAfter`, `diff().affectedKeys().hasOnly`, `list.concat`, `'k' in map`, `string.lower().split('@')[1]`, and Storage `firestore.get/exists(/databases/(default)/...)` are all valid. On line 133-134, `==` binds tighter than `&&`, so `(scope == 'PUBLIC') == existsAfter(...)` is evaluated as intended. Rules on a missing document fail closed: `getAfter(...).data` on null is an error, and an error means deny. None of this ran in the Rules Playground or the emulator (0010 says so).

## What was verified by reading
- **Build (slice 1).** The plugin is `apply false` at the root and applied in `:app`. The BoM is a `platform`, with no versions on the four Firebase artifacts. `kotlinx-coroutines-play-services` gives `await()`. The catalog style matches the existing entries. BoM 34.x already folds the KTX extensions into the main artifacts, so the `com.google.firebase.firestore.firestore` etc. imports are right. minSdk 24 is above Firebase's 23. Coroutines core went from 1.9.0 to 1.11.0 transitively (recorded in state.md). The build has no compiler warnings.
- **Container (slice 2).** It follows the plan exactly. There are four `by lazy` properties. `firestore` applies `persistentCacheSettings { }` (100 MB default) inside the `lazy` block, so it runs before any other call on the instance. There is no `GlobalScope` and no blocking call. The KDoc explains the pattern (decision 0009, Contacts_#3) in words both authors can defend. `ticketsRepository` is still the local one (F-011 swaps it). `MainActivity` is unchanged, and no smoke code is left (`f010-smoke` grep is empty).
- **MVVM / UI / i18n / accessibility / orientation / permissions.** No UI, string, ViewModel or permission change. `INTERNET`/`ACCESS_NETWORK_STATE` come from the Firebase manifests (normal permissions, no runtime prompt). FCM auto-init gets a token at first start, as 00-plan says. `POST_NOTIFICATIONS` is not needed until F-016. Nothing to review for i18n, a11y or rotation.
- **Anonymous users (CLAUDE.md rule).**
  - Anonymous `read` is granted only on `publicTickets`, `categories` and `municipality`.
  - `tickets`, `ticketLocations`, `users`, `messages` and `nifs` all need `request.auth != null`.
  - `publicTickets` `create` whitelists the eight keys, so `creatorId`, `description` and the exact `location` cannot be stored there. `update` is limited to `currentStatus`/`thumbnails`/`resolvedAt`.
  - The brief (I.1) defines anonymous users as *not authenticated*, which matches the rules' assumption that they never sign in. The assumption is not written down (S4).
  - Media of public tickets are readable by everyone, as the Gate-approved table says ("thumbnails and media"), and as brief I.1.C wants ("apenas com imagem(ns) e/ou vídeo(s)"). Photo GPS EXIF is stripped (F-009), but video location metadata is not (S5).
  - The rules do not check the rounding of `coarseLocation`. That is recorded in 0010 as a convention: `toAnonymousView()` is the only writer.
- **Schema vs Gate 1.** Every row of the 00-plan table appears in 0010 with the same fields. Q8 (`ticketLocations`, never anonymous), Q9 (`participantIds`), Q10 (no `AttachmentPhase` in `model/`) and Q11 (`stats/summary` reserved and denied) are applied. Q12 is applied as decision 0011, which records Q1-Q7 as answered. The writer columns of `tickets` and `publicTickets` were widened (S1).
- **Persistent cache vs brief IV.** It is on, with the default size. The "transactions only online, batches offline" choice in 0010 is consistent with offline reporting and following.
- **Recorded deviations.** BoM 34.19.0 (R2, with the compiler error quoted), slice order, commit message and the smoke test not run are all logged in state.md with reasons. I accept the first three. The smoke test is S6.

## Blocking
1. **Self-service operator accounts: the email domain is checked but not owned** (`firestore.rules` line 75-76, `isOperator()` line 31-33). Firebase email/password sign-up does not prove the user owns the address. Anyone can create `someone@<emailDomain>`, write `users/{uid}` with `role: 'OPERATOR'`, and pass the `split('@')[1] == municipality().emailDomain` check. From then on `isOperator()` is true. That account can read all `users` documents (NIF, phone, georeferencing, email), all tickets with exact coordinates and messages, and every Storage file. It can also change any ticket's lifecycle. Brief, operator A (I, second "2."), asks that the operator email "pertence ao domínio da autarquia". Only a verified address proves that.
   **Fix:** require a verified email for the operator role, both at registration and on every check. In `isOperator()`:
   ```
   return hasUserDoc() && userDoc().data.role == 'OPERATOR'
     && request.auth.token.email_verified == true;
   ```
   and in the OPERATOR branch of `users` `create` add `&& request.auth.token.email_verified == true`. Apply the same `email_verified` condition in `storage.rules` `isOperator()`. In 0010 (choice "One municipality", risk 5), write that operator registration sends the verification email and creates `users/{uid}` only after verification. MFA enrolment needs a verified email anyway, so D-009 has to do this step regardless. Also state that `emailDomain` is stored lowercase without `@` (for example `cm-coimbra.pt`), because the rule compares it with `email.lower().split('@')[1]`. Tell Diogo, because this changes D-009's registration order.

## Should-fix
1. **The lifecycle writers were widened beyond the Gate-approved schema** (`firestore.rules` line 148-150 and 173-174; 0010 table rows `tickets` and `publicTickets`). Gate 1 approved "operators for lifecycle changes" and "`publicTickets` ... operators afterwards". The code and 0010 now say "operators **and participants**". With that, any participant can:
   - set `currentStatus` to any value;
   - rewrite past `statusHistory` entries (only the size is checked);
   - replace or empty `attachments`, including the operator's resolution proof;
   - change a CLOSED ticket, although brief II.5 says CLOSED is "bloqueado".

   Combined with S2 (any active citizen can become a participant), any active citizen can resolve or close any ticket. state.md logs this as "detail filled in beyond the plan", but not as a deviation the person approved.
   **Fix:** keep the participant path, but narrow it to what Q4 and reopening need. Then log it as a deviation and ask the person to OK it, or go back to operators only. A narrow version:
   ```
   function historyAppendedBySelf() {
     let old = resource.data.statusHistory;
     let neu = request.resource.data.statusHistory;
     return neu.size() == old.size() + 1
       && neu[0:old.size()] == old
       && neu[old.size()].responsibleId == request.auth.uid
       && neu[old.size()].status == request.resource.data.currentStatus;
   }
   allow update: if resource.data.currentStatus != 'CLOSED'
     && changedKeys().hasOnly(['currentStatus', 'statusHistory', 'attachments'])
     && historyAppendedBySelf()
     && (isOperator()
         || (isParticipant(resource.data)
             && resource.data.currentStatus == 'RESOLVED'
             && request.resource.data.currentStatus in ['OPEN', 'CLOSED']));
   ```
   Add a separate attachments-only rule (`changedKeys().hasOnly(['attachments'])`, size not shrinking) for the F-013 upload state. Do the same for `publicTickets` `update` (participants only `currentStatus` in `['OPEN', 'CLOSED']`). Update the 0010 table and its Consequences bullet to match.
2. **Any active citizen can follow any ticket, and following grants full read** (`firestore.rules` line 138-142). The follow rule checks neither scope nor proximity. `ticketLocations` lists every ticket id, PRIVATE ones included. So an active citizen can follow every ticket in a loop and then read every description, message thread and media file, including the PRIVATE tickets 0003 makes private by default. The 10 m check exists only in the client. 0002 left "which author's scope wins" open, and 0010 does not answer it.
   **Fix:** pick one of these and record it in 0010 (Choices and Consequences):
   - (a) Only PUBLIC tickets can be joined. Add `scope` to `ticketLocations` and `&& resource.data.scope == 'PUBLIC'` to the follow rule. F-012 then groups only PUBLIC reports, and a PRIVATE report always opens its own ticket.
   - (b) Keep it open and write the risk down explicitly as accepted for the prototype ("an active citizen who knows a ticket id can follow it and read its thread").

   (a) is the one that is easy to defend.
3. **`nifs` can be squatted** (`firestore.rules` line 97-99). Any signed-in user can create `nifs/CITIZEN_<any nif>` with their own uid, without a `users` document. They can create many of them, and that blocks the real owner's registration forever (`update`/`delete` are false). The `users` rule checks the NIF document, but the NIF rule does not check the user.
   **Fix:** tie the NIF document to the user document written in the same transaction:
   ```
   allow create: if isSignedIn()
     && request.resource.data.keys().hasOnly(['uid'])
     && request.resource.data.uid == request.auth.uid
     && !exists(userPath(request.auth.uid))
     && getAfter(userPath(request.auth.uid)).data.role + '_'
          + getAfter(userPath(request.auth.uid)).data.nif == nifId;
   ```
   Optionally also validate `nif is string && nif.matches('^[0-9]{9}$')` and `role in ['CITIZEN', 'OPERATOR']` in the `users` create.
4. **Profile editing lets a citizen change their name, and the anonymous assumption is not written down** (`firestore.rules` line 80-83).
   - Brief I.2.C: "Nome, NIF e georreferenciação não poderão ser alterados". Brief, operator C: "Nome e NIF". The forbidden-keys list has `nif` and `georeferencing` but not `name`.
   - Separately, every `isSignedIn()` grant (for example `nifs` `get`/`create`) would also accept a Firebase *anonymous-auth* token if someone ever turned that provider on.

   **Fix:**
   - Add `'name'` to the `hasAny([...])` list.
   - Make `isSignedIn()` `request.auth != null && request.auth.token.firebase.sign_in_provider != 'anonymous'`.
   - Add a line to 0010: "anonymous users do not sign in (brief I.1, 'não autenticados'); the Anonymous Auth provider stays disabled".
5. **No size limit on Storage uploads, and video metadata reaches anonymous users** (`storage.rules` line 34-35 and 41-43; 0010 Storage row). On Blaze, an unbounded upload is a cost risk that the budget alert only reports after the fact. Public videos are readable by everyone. FileUtils strips EXIF GPS from photos only, and an MP4 from the camera can carry a location atom (`©xyz`). That is exact coordinates for an anonymous user.
   **Fix:**
   - Add size limits now, for example `&& (request.resource.contentType.matches('image/.*') && request.resource.size < 5 * 1024 * 1024 || request.resource.contentType.matches('video/.*') && request.resource.size < 50 * 1024 * 1024)` on `tickets/...`, and `request.resource.size < 2 * 1024 * 1024` on the profile photo. Pick the numbers with F-013's compression targets.
   - In 0010 Consequences, add: "F-013 must remove video location metadata (or re-encode) before upload, because public media are anonymous-readable". If F-013 cannot do that, restrict the anonymous Storage read to `fileName.matches('.*_thumb[.]jpg') || fileName.matches('.*[.]jpg')`.
6. **The acceptance checks "writes/reads a test document" and "offline persistence is on" were not run.** The state.md reason is "no device attached", but an AVD would do. Two of the task's four acceptance checks are unmet. Test mode expires on 2026-11-21 (0011), so a mistake in the console setup (wrong database location, rules mode) would only show up in F-011.
   **Fix:** before `/push-task`, run the plan's slice 2 smoke test on an emulator. Write and read `debug/f010-smoke`, log `isFromCache`, read once more in airplane mode, revert the code and delete the document. Record the result in state.md. If the person prefers to defer it, add it as an explicit acceptance check to F-011's task file and mark these two checks "deferred to F-011" in state.md.

## Nit
1. The operator validation (`firestore.rules` line 86-89) accepts any `registrationStatus` value and any target state. **Fix:** add `&& resource.data.registrationStatus == 'PENDING' && request.resource.data.registrationStatus == 'ACTIVE'`.
2. The OPERATOR branch of `users` create does not forbid citizen-only keys, and neither branch whitelists keys. **Fix:** add `request.resource.data.keys().hasOnly([...])` per role, using the 0010 field list.
3. `tickets` create does not check that the category is active (brief, operator E: "só podem abrir ... tipos ... mantidos ativos") or that at least one attachment exists (brief I.2.E: "deve ser carregada uma imagem/vídeo"). **Fix:** `&& get(/databases/$(database)/documents/categories/$(request.resource.data.categoryId)).data.active == true && request.resource.data.attachments.size() >= 1`. The batch stays under the 20-call limit.
4. Messages can still be created on a CLOSED ticket (line 156). **Fix:** `&& get(ticketPath(ticketId)).data.currentStatus != 'CLOSED'`, or leave it to D-008 and say so in 0010.
5. The Storage participant check reads `tickets/{id}` on the server. If the F-013 worker starts before the queued report batch has synced, the upload is denied. **Fix:** add to 0010 Consequences: "F-013 worker retries with backoff on PERMISSION_DENIED (ticket not yet synced)". Also note that the first deploy of cross-service Storage rules asks to grant the Storage service account Firestore access in the console (a step for the person, 0011 point 5).
6. `CityFixApplication` KDoc. The guarantee "settings applied before any other Firestore call" holds only while nobody calls `Firebase.firestore` directly. **Fix:** add one sentence: "Always use `firestore` from here; calling `Firebase.firestore` elsewhere first would throw on the settings." Keep it in mind for D-009/F-011.
7. The owner update rule blocks `email`, but the brief lets the user change data other than name/NIF/georeferencing. **Fix:** either document in 0010 that the email changes only through Auth (and the document follows `token.email`), or allow `email` when `request.resource.data.email == request.auth.token.email`.
