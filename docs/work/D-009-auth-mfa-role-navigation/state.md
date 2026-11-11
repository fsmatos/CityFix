# D-009: Login, MFA, password recovery and role navigation
Branch: dev/Diogo · Base: f0131b1
Remote: origin/dev/Diogo

| Phase | Status | Date | Notes |
| --- | --- | --- | --- |
| Start | done | 2026-11-11 | After merging origin/dev/Francisco (F-010) ahead of main |
| Plan | done | 2026-11-11 | 00-plan, 01-coding-plan, 03-docs-plan; 6 slices, no tests; Q1-Q13 for Gate 1 |
| Gate 1: plan approved | done | 2026-11-11 | Approved by Diogo; Q1 MFA mandatory for both roles (brief I.2.B, I.3.B); Q7 nobody expands (deviation from the plan); Q2-Q6, Q8-Q13 as recommended |
| Code | pending | | |
| Review | pending | | |
| Docs | pending | | |
| Pushed | pending | | |
| Merged | pending | | |

## Log
Append-only.

- 2026-11-11 Sync: origin/main had nothing new for dev/Diogo. F-010 (needed by D-009) was reviewed and pushed on origin/dev/Francisco but not merged into main; Diogo chose to merge origin/dev/Francisco into dev/Diogo now (f0131b1, 16 commits). Conflicts only in `docs/architecture/overview.md` (2 hunks) and `docs/report/notes.md` (1 hunk), resolved by keeping both sides. Decision number clash: F-010 and D-008 both used 0011; D-008's record renumbered to `0012-ticket-lifecycle-rules.md` and its references updated (Francisco's files untouched). assembleDebug and lint pass: 0 errors, 18 warnings (F-010 recorded 17; the extra one is a newer-version warning, all 18 are version or unused-resource warnings). `app/google-services.json` present locally (git-ignored).
- 2026-11-11 Found while merging, not D-009 scope: F-010's handover (overview "Handover (F-010)") says the reopen window, CLOSED-on-read and 24 h warning are computed from a server-written `resolvedAt` field, and that every status change has `responsibleId` = the caller, with participants closing after 5 days. D-008 (decision 0012) computes them from the last RESOLVED history entry and writes the automatic close with `responsibleId = "system"`. To reconcile in F-011 / D-013 with Francisco.
- 2026-11-11 Start: task file `docs/tasks/phase-2/D-009-auth-mfa-role-navigation.md` (target 2026-11-13).
- 2026-11-11 Plan: planner wrote the three plans. 6 slices: repositories, session above the NavHost, login/forgot password, role navigation with placeholders, account setup (verify e-mail + enrol phone), SMS code at login. MFA API checked on the Firebase docs for BoM 34.19.0. Deviation from the task file: separate auth and profile flows in a sealed `Session` instead of `currentUser: Flow<User?>`. Console steps (Identity Platform, SMS MFA, test numbers, SHA fingerprints, three test users with `users/{uid}`) are for Diogo. Brief not checked (PDF unreadable); Contacts_#3 not read by the planner (no shell), it followed the patterns already cited in the code.
- 2026-11-11 Brief section I pasted by Diogo in chat (the PDF is still unreadable here). It confirms: I.2.B and I.3.B "e-mail/password, com duplo fator (MFA) e recuperação de credenciais por e-mail" for the registered citizen and the operator (factor type not specified, so F-010's SMS stands); I.1.B anonymous list "categoria, situação e thumbnail"; I.2.D citizen gets the anonymous features; I.2.E citizen's own list "com o estado de cada uma" and reopening "se dentro de 5 dias após conclusão"; I.3.A operator registration "fica logo ativo" and e-mail in the municipality's domain (D-010); I.3.E the operator's full view (coordinates, author contacts, full history with media) is the "Painel de Gestão Operacional" (D-013); I.2.A the citizen registration "fica pendente até validação" (the brief does not say whether a pending citizen may log in; Q2 stands as recommended).
- 2026-11-11 Gate 1: approved by Diogo. Q1: MFA mandatory for both roles, enrolment in D-009 (brief I.2.B/I.3.B); the `sign_in_second_factor` rule only at the deploy step with Diogo's OK. Q7: **deviation from the plan's recommendation** — nobody gets the expand toggle on the public list: `canExpandCards` stays `false` for every session (the list is always projected, so an expanded card would add only the date; the operator's full view is D-013's panel, brief I.3.E). The implementer keeps the D-007 flag and its `&&`, with the comment updated, instead of deriving it from the role. Q2-Q6 and Q8-Q13: the recommended answers.
- 2026-11-11 Brief check for D-008 (decision 0012), from the same paste: I.3.E "pedido de dados" is an operator action, consistent with Q1 (a citizen question does not set INFO_REQUESTED); I.2.E puts reopening among the citizen's own tickets ("ocorrências do utilizador"), and the I.2 Obs says several users "acompanham" a grouped ticket, so creator + followers (Q2) is a reasonable reading, not literal. Sections II, III and I.3.E's wording on proof ("carregamento de foto/vídeo do trabalho concluído") support the proof rule; the 5-day close and 24 h warning sections were not pasted.
