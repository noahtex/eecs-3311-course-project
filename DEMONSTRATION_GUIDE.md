# Planit Stage 2 demonstration guide

Prepared November 15, 2026, for the November 15 deadline. This is a recording script, **not a record of successful demonstrations**. All results below are expected results to verify on screen. No live API requests were run to prepare this document. Generated titles, IDs, prices, assignments and narratives must come from the application; none are supplied here as fabricated outputs.

## 1. Setup and recording plan

Use JDK 21 and Maven, a desktop display, and a terminal whose working directory is `ai-event-planner`. From the repository root:

```powershell
Set-Location ai-event-planner
java -version
mvn -version
# Isolated recording data, outside the repository; use a new folder for a fresh take.
$env:PLANIT_DATA_DIR = Join-Path $env:LOCALAPPDATA 'PlanitDemo-2026-11-15-GUI'
mvn compile
mvn javafx:run
```

Expected: Java/Maven report their versions, compilation completes, and the **Planit** JavaFX dashboard opens. `pom.xml` configures `com.eventplanner/com.eventplanner.App`; this opens the dashboard, not the unused starter FXML screens. Build dependencies may need downloading. Do not use an unverified `java -jar` launch.

Close the GUI normally and wait for Maven to return before starting the CLI. The actual separate-JVM execution configured in `pom.xml` is:

```powershell
mvn compile exec:exec@cli
```

Expected: the CLI prints its storage description and `planner>` prompt. Enter `help`. Use `exit` to close cleanly. Prefer this launcher to legacy `exec:java`, which can produce the documented Okio daemon cleanup warning.

**Exclusive storage lock:** GUI and CLI cannot open the same store simultaneously. The second process should report a storage error. Do not remove `planit.lock` to bypass it: the file can remain after exit; the OS lock is what matters. Default Windows storage is `%LOCALAPPDATA%\Planit\planit.json`; the environment override above isolates recording data. Set the override in every new terminal before launching. A Maven `-Dplanit.dataDir` argument is not a reliable substitute for passing a property to the actual child JVM.

Configure `GEMINI_API_KEY`, optional `GEMINI_MODEL`, and `GOOGLE_PLACES_API_KEY` privately in the application working-directory `.env` or environment before recording. Environment settings take precedence; restart after changes. Places requires Places API (New) and applicable billing configuration. Do not display `.env`, terminal environment dumps, keys or billing pages.

### Reliable sequence and preparation

1. Record GUI F01–F06, then F09, F07, F08, F10, followed by persistence. F09 comes early so F08 can visibly invalidate an approved communication. Label clips by feature even though recording order differs.
2. Record CLI in a **separate fresh store** using the same event inputs, with the same sequence. Change the override suffix to `CLI` before launch. This makes each interface's creation/generation evidence clear. For the cross-interface persistence check, reopen the GUI store sequentially in CLI instead.
3. Before the take, prepare this script, fictional profiles, and a place to copy IDs. Check configuration privately. Allow time for genuine provider failures and manual repairs; calls may take around a minute and malformed responses may cause retries. Never edit JSON to manufacture successful generations.
4. Generate one concept set, one budget-priority blueprint set, one task plan, one schedule and one budget per interface when possible. Inspect alternatives instead of generating every strategy. Prepare availability ahead of time; it avoids repeated schedule repair. If recordings use genuinely saved results prepared earlier, say so and show the saved records rather than presenting them as newly generated.
5. Pause or trim API waits transparently. If a request fails, show the safe error and mark that part incomplete. A saved-result walkthrough does not demonstrate a new successful provider request. No production offline/mock switch exists for completing every generation without live services.

### API/quota map

| Action | Live service / possible quota use |
| --- | --- |
| F01 fuse; F02 generate; F03 generate tasks; F04 build agenda | Gemini |
| F05 generate budget/catering | Google Places catering search and Gemini; provider failure can yield explicitly unverified generic planning |
| F06 research / compare | Research: Google Places; comparison: Gemini, even for manual candidates |
| F07 simulate | Gemini interpretation/analysis |
| F08 GUI preview / CLI natural update; contingency generation | Gemini; GUI preview also analyzes impacts. CLI structured update, confirmation and Observer checks are local |
| F09 generate communication / FAQ | Draft generation: Gemini; FAQ: local deterministic whitelist |
| F10 analyze / record actuals | Analysis: Gemini; recording actuals: local |
| Create, configure, list/view, edit, validate, approve/discard, reopen | Local storage/validation; no provider request |

Running the live portions during recording may consume API quota or incur charges. This guide does not authorize or execute them now.

## 2. Sample data

All people below are fictional. The event is a demonstration scenario. Date/time values are **event-local Toronto wall time**; the application stores no timezone ID and performs no zone/DST conversion.

| Field | Exact input |
| --- | --- |
| Name / type | `YURUN Winter Social` / `Club social` |
| Location | `York University, Toronto` |
| Start / end | `2026-11-20T17:30` / `2026-11-20T21:00` |
| Attendance / spending limit / currency | `80` / `1000` / `CAD` |
| Constraint | `Accessible indoor venue; dietary-inclusive refreshments; CAD 1000 maximum; limited organizer and venue availability. Optional run only if safe; indoor participation must remain available.` |
| Alex Chen idea | `Social evening with icebreakers and relaxed indoor conversation.` |
| Maya Singh idea | `Optional short group run, with an equivalent indoor activity for non-runners.` |
| Jordan Lee idea | `Affordable catering with vegetarian, vegan, halal and gluten-free options; confirm allergies with the provider.` |
| Dietary list | `vegetarian, vegan, halal, gluten-free` |
| Catering needs | `Affordable refreshments for 80; label ingredients and confirm allergy handling with provider.` |
| Venue query | `Accessible indoor community event space near York University Toronto for 80 people with affordable rental` |
| Equipment / space | `microphone` / `social-room` (planning resource name, not a booked room) |

Fictional member profiles (CSV skills/preferences fields):

| CLI ID | Name | Skills | Preferences | Availability | Existing hours / capacity |
| --- | --- | --- | --- | --- | --- |
| `alex-demo` | Alex Chen | `coordination, logistics` | `venue, setup` | Nov 16, 17:00–21:00; Nov 20, 17:30–21:00 | `0` / `8` |
| `maya-demo` | Maya Singh | `facilitation, safety` | `icebreakers, group run` | Same two intervals | `0` / `8` |
| `jordan-demo` | Jordan Lee | `budgeting, catering` | `refreshments, dietary inclusion` | Same two intervals | `0` / `8` |

Configure fictional speaker `host-demo` / `Taylor Brooks` for Nov 20, 17:30–21:00, plus `social-room` and `microphone` resources for the same interval. GUI generates person IDs: use names in its selection controls; CLI IDs above apply to the separate CLI take. Review generated tasks for actual skill/effort fit; these profiles do not guarantee every proposed assignment is feasible.

### Explicitly fictional actual outcomes for F10

State aloud: **“The event is still in the future. These are fictional actual outcomes solely for demonstrating post-event analysis.”**

- Actual attendance: `72`; actual expenses: `920.00` CAD.
- Choose real approved task IDs from this demo: mark all but one completed and the remaining task unfinished. If there is only one task, mark it completed; explain the different completion rate. Never invent task IDs or use draft-only tasks.
- Organizer feedback: `FICTIONAL - Alex Chen: Setup was smooth and refreshments stayed within the spending limit.`
- Attendee feedback: `FICTIONAL - Sample attendee: Icebreakers were welcoming, but refreshments arrived late.`
- Problem: `FICTIONAL - Catering arrived 15 minutes late; the optional run was replaced by an indoor activity.`

Expected arithmetic with unchanged target 80: attendance variance `72 - 80 = -8`. Expense variance is `920.00 - approved budget subtotal`, **not** automatically `920 - 1000`. The spending-limit difference is separately `-80`. Task completion depends on the real approved tasks selected. Qualitative conclusions are generated, not predetermined.

## 3. GUI demonstration

For each action, wait for background completion and inspect the footer/error message. Use calendar pickers plus 24-hour hour/minute selectors; ISO values in this guide describe the exact dates/times to choose. Availability uses **Add availability interval**, with From/Until calendars, not pasted multiline text.

### Event setup

Open **Events → Create event**. Enter the event fields from Section 2, including both start and end. Save, select its row and click **Set active event** if needed. Use **Add goal / constraint** to save the constraint.

Expected: a stable event ID and correct active-event details, attendance 80 and limit 1000. Use CAD in Budget configuration before discussing currency. An event is local saved data, not a booking.

### F01 — Ideas

Use **Add organizer idea** three times with the exact organizer/idea pairs. Click **Fuse ideas with Gemini**. Inspect at least two returned concept alternatives and their source ideas, themes, conflicts, compromises and uncertainty.

Expected: saved attributed ideas and genuine generated concepts that can be reviewed for optional-run/inclusion trade-offs. If the request fails, no successful synthesis should be claimed. Source idea/constraint edit/delete and concept editing/approval are unavailable.

### F02 — Blueprints and feasibility

Choose a generated concept in **Blueprints**, select priority `budget`, and click **Generate alternatives**. Inspect agenda, checklist, resources, assumptions, capacity/attendance, estimated budget and feasibility warnings. Choose a suitable row → **Select blueprint** → **Approve selected blueprint**. Cancel the dialog once; verify approval is unchanged. Repeat and confirm.

Expected: the execution card names the approved blueprint; alternatives remain. Only this approved blueprint supplies new F03/F04 execution context. Blueprint content cannot be edited. If unsuitable, generate another set or explain the constraint; approval does not verify prices or availability. When alternatives exist without approval, downstream generation is blocked before an AI request. Do not rely on the separate no-blueprint event-only fallback for this take.

### F03 — Tasks and deadlines

In **Tasks & team**, **Add team member** for each profile above; add both known availability intervals. **Generate task proposal**. Inspect assignees, required skills, effort, dates and warnings; click **Validate tasks**.

Select a suitable logistics task → **Edit assignment** → Alex Chen, start Nov 16 17:00, deadline Nov 16 21:00. Use these dates only if its effort fits four hours and its skills match; otherwise retain appropriate generated values or choose another task. Validate again, review warnings and **Approve draft**, confirming the dialog. Select a task → **Change status** → `IN_PROGRESS`; inspect the draft, then **Discard draft** and confirm.

Expected: generation/edits remain drafts; approval stores reviewed tasks; discarding the later status edit restores the approved version. Unknown availability/capacity and unsuitable assignments are visible diagnostics. The application permits deliberately unassigned tasks with warnings; do not describe approval as proof every task is staffed. Individual task creation/deletion or title/effort/skill editing is unavailable.

### F04 — Schedule and conflicts

In **Schedule**, verify end Nov 20 21:00; **Configure speaker** and **Configure resource** using Section 2. **Generate agenda**; inspect setup, activity, break and teardown within 17:30–21:00. Use **Validate schedule**.

Select one session; note its original values. **Edit selected session**: temporarily set start Nov 20 17:00 and end 17:20, retaining its kind/speaker/space/resources. Validate and attempt **Approve schedule**.

Expected: outside-window diagnostics and blocked approval. Restore that session's original values, fix any other genuine conflicts or unknown resource availability, validate, then approve and confirm. No session add/delete exists. If generated names introduce unconfigured resources, configure them or revise sessions to the known resources. All setup/teardown must also fit inside the declared window.

### F05 — Budget and catering

In **Budget & catering → Configure catering needs**, enter CAD, the dietary CSV and catering text. Select `budget` priority → **Generate budget**. Inspect allocations, provider evidence, dietary assumptions and UNKNOWN versus ESTIMATED prices.

Choose an allocation → **Edit allocation**. First set quantity `1`, status `UNKNOWN`, blank unit cost and source `Unknown cost for demonstration`; retain its category/description. **Recalculate & validate**, then attempt approval. Expected: unknown costs prevent approval.

Edit again using a defensible **ESTIMATED** unit cost and explicit source `Fictional demonstration planning estimate; not a supplier quote`. Retain a realistic category/description and quantity; choose the amount only after reading the other actual allocations so the whole known total stays within CAD 1000. Do not invent a quote or label an estimate CONFIRMED. Validate, review all remaining unknowns/shortfalls and approve only if valid. Expected: Java totals and remaining funds update; estimates stay estimates after approval. If the proposal cannot be repaired through supported edits, show the limitation instead of claiming approval. No allocation add/delete or catering recommendation editing exists.

### F06 — Venue research

In **Venue research**, **Configure venue requirements**: wheelchair entrance `yes`, equipment `microphone`. Enter the venue query → **Research venues**. Inspect address, source/retrieval time, available contact/website information and unknown cost/capacity/availability. Compare at least two actual candidates with **Compare suitability**.

Expected: Places listings and deterministic known-fit ranking with Gemini explanations; missing attributes remain unknown. If too few candidates/provider failure, add the two explicitly fictional manual candidates in the CLI sample below through **Add manual venue** using ESTIMATED evidence, then compare. Label that as a manual-entry demonstration, not live discovery. First ten Places results only; no venue selection/booking or candidate edit/delete. A wheelchair entrance does not establish full accessibility. Do not spend recording time opening third-party pages unless necessary.

### F09 — Communications and FAQ (record before F08)

In **Communications**, choose `EMAIL`, tone `friendly and concise` → **Generate communication draft**. Inspect facts. In the editor use `Join YURUN Winter Social, a Club social, on November 20, 2026 at 5:30 PM (Toronto event local time).` → **Save draft edits** → **Approve draft**, confirm. Save a small further edit, verify it returns to DRAFT, then review/reapprove. Optionally demonstrate INSTAGRAM/WHATSAPP by generating additional drafts; each consumes quota.

Ask **Attendee FAQ**: `When is the event?`, then `What is the registration URL?`.

Expected: the first answer uses saved start `2026-11-20T17:30`; the second says the information is not confirmed. Only exact narrow name/type/date intents are supported; even end-time/accessibility questions can return unknown. Unsaved edits block approval. Edited factual claims require human checking. No email/social sending exists, and channel formatting is limited.

### F07 — Hypothetical change, no real mutation

Show current attendance 80 and limit CAD 1000. In the scenario screen enter `What if attendance increases to 100 and the spending limit becomes CAD 1200?` → **Run simulation**. Inspect interpreted fields, before/after, affected components and warnings; verify the interpretation matches the request.

Expected: only the snapshot becomes 100/1200. Spending-limit delta is +200 if interpreted accordingly; expense delta is a rough attendance-proportional estimate using approved spending, not a quote and not the limit delta. It scales fixed costs too. Navigate back to Events: still 80/1000. **Clear simulation**, then perform the F07 persistence check in Section 5 **before F08**. Simulation results are transient and not saved.

### F08 — Proposed real update, cancellation and Observer checks

In **Plan Updates & Contingencies**, enter `Change the event spending limit to CAD 900. Keep attendance, date, time and all other fields unchanged.` → **Preview changes & impacts**. Verify only budget changes. **Confirm Changes**, cancel the dialog; verify real limit remains 1000. Click **Discard proposed changes** to reject the pending proposal. Preview the same request again, inspect and confirm it.

Expected: the real limit becomes 900 only on confirmation. Show **Plan revalidation**, then Tasks/Schedule/Budget warnings and validate their retained plans; in Communications show the formerly approved draft invalidated. Task/schedule/budget Observers recheck known stored data; they do not regenerate plans. If approved budget subtotal exceeds 900, show that actual shortfall; otherwise report the actual diagnostics without inventing a conflict. Preview/cancellation changes no real facts; cancelling the GUI dialog retains a proposal until discarded.

Generate contingency for `Catering delay and unsafe outdoor running conditions`; inspect recommendations/assumptions. Expected: stored recommendations, no weather forecast/provider availability lookup and no automatic application. To keep the intended event baseline, propose `Restore the event spending limit to CAD 1000. Keep all other fields unchanged.`, verify and confirm. Review/reapprove affected drafts as needed. Historical review warnings may remain after repairs; explain that limitation.

### F10 — Fictional post-event outcomes

Read the fictional-outcomes statement aloud. In **Post-Event** enter Section 2 actuals/feedback/problems; mark outcomes against the displayed **approved** tasks. **Save actual outcomes**, review and confirm, then **Generate post-event analysis**. Inspect variances, completion, missing information, conflicting feedback and lessons; show report history.

Expected: saved fictional outcomes and a genuine generated report using them. Unsaved changes block analysis. Blank actuals mean UNKNOWN, not zero. Baseline is the current approved plan at analysis time, not a frozen event-close snapshot. Keep CAD unchanged: reports/actuals lack historical currency identity and GUI history uses current currency.

## 4. CLI demonstration: exact commands and prompt inputs

Commands below are entered at `planner>`, not PowerShell. Replace `E`, `C`, `B`, `T`, `S`, `I`, `M` with IDs actually printed by the application: event, concept, blueprint, task, session, budget item, communication. These are notation, not shell variables. Replace every occurrence; never type angle brackets or literal placeholder letters. `[blank]` means press Enter without text. Commands are case-sensitive; free text belongs in the subsequent prompt, not on the command line. Slash notation below lists separate valid commands.

### Create event and F01

Run `event create`; answer in order:

```text
YURUN Winter Social
Club social
York University, Toronto
2026-11-20T17:30
80
1000
```

Copy E. Run `schedule window E`; enter `2026-11-20T21:00`. Run `constraints add E`; enter the exact constraint from Section 2. Run `ideas add E` three times, each supplying Organizer name then Idea from Section 2. Then:

```text
event view E
constraints list E
ideas list E
ideas fuse E
concepts list E
```

Expected: saved event/end, three attributed ideas, constraint and genuine Gemini alternatives with IDs. Copy C from a reviewed concept. No approval/edit command for concepts exists.

### F02 — Generate, select and approve

```text
blueprint generate E C
```

Priority prompt: `budget`. Then `blueprint list E`, `blueprint view E B`, `blueprint select E B`, `blueprint approve E`. At approval enter `no`; inspect unchanged approval. Run `blueprint approve E` again; enter `yes`, then `blueprint list E`.

Expected: pending selection is distinct from approved execution status; cancellation preserves prior approval, confirmation approves the reviewed alternative, and others remain. Do this before F03/F04.

### F03 — Profiles, draft, edit, validate and approve

Run `team set E` for Alex with these exact prompt responses, in order:

```text
alex-demo
Alex Chen
coordination, logistics
venue, setup
yes
2026-11-16T17:00
2026-11-16T21:00
yes
2026-11-20T17:30
2026-11-20T21:00
no
0
8
```

Repeat `team set E` for Maya/Jordan, substituting their first four profile fields from Section 2 and keeping the remaining responses. `team list E` verifies them. Run `tasks generate E`; answer `no` to the approval prompt to retain the draft. `tasks list E` shows IDs and validation warnings; there is **no `tasks validate` command**.

For a fitting logistics task, `tasks assign E T` prompts for member ID, work start, deadline, then draft approval. Enter `alex-demo`, `2026-11-16T17:00`, `2026-11-16T21:00`, `no`, subject to the same effort/skill checks as the GUI. Inspect `tasks list E`; correct genuine warnings, then `tasks approve E` (no additional yes prompt). Run `tasks status E T`, enter `IN_PROGRESS`, then `no`; `tasks discard E`, then `tasks list E`.

Expected: validated draft/approved transitions and discard preserving approved task status. `tasks assign/status` always asks whether to approve after the edit; `tasks approve` directly attempts approval. Do not pipe a fixed full script through unpredictable generated outputs.

### F04 — Availability and agenda

Run `speaker set E`; answer `host-demo`, `Taylor Brooks`, `yes`, `2026-11-20T17:30`, `2026-11-20T21:00`, `no`.

Run `resource set E` twice: first answer `social-room`, then availability `yes`, start `2026-11-20T17:30`, end `2026-11-20T21:00`, `no`. Repeat with `microphone`. `schedule build E` → answer `no` at draft approval. `schedule view E` and `schedule conflicts E` show genuine sessions and diagnostics.

Copy S and its original values. `schedule edit E S` prompts for title, kind, start, end, speaker, space, extra resources, approval. For a temporary invalid session use:

```text
Demonstration session outside window
ACTIVITY
2026-11-20T17:00
2026-11-20T17:20
host-demo
social-room
microphone
no
```

Run `schedule conflicts E`, then `schedule approve E`: expected rejection for outside-window/unavailable times. Run `schedule edit E S` again and restore **all original values** (blank speaker/resources where originally empty), answer `no`, fix other genuine issues, inspect conflicts and `schedule approve E`. Expected: approval only after blocking conflicts are resolved. `schedule discard E` is available to abandon a draft while keeping a prior approved schedule; it does not restore an unapproved first draft.

### F05 — Budget/catering

`budget generate E` prompt answers: `CAD`; `vegetarian, vegan, halal, gluten-free`; the catering-needs sentence from Section 2; `budget`. Then `budget view E` and `budget validate E`.

`budget edit E I` prompts: Category, Description, Quantity, Price status, Unit cost **only if not UNKNOWN**, Source. To demonstrate blocking unknowns enter:

```text
Refreshments
Demonstration allocation pending estimate
1
UNKNOWN
Unknown cost for demonstration
```

`budget validate E`, then `budget approve E`: expect blocked approval. Edit I again, retaining appropriate category/description, positive quantity and `ESTIMATED`, then enter a non-negative unit cost chosen after reading the real other allocations, and source `Fictional demonstration planning estimate; not a supplier quote`. There is no universally safe fixed price for unpredictable generated allocations. Resolve other UNKNOWN items/overruns through supported edits, validate, then approve. `budget approve E` displays and directly attempts approval; no yes prompt. `budget discard E` abandons later edits. Expected: subtotal/remaining computed from real quantities/costs; approved estimates remain unverified. Do not substitute spending limit for approved expense baseline.

### F06 — Research and comparison

`venues research E` → enter the exact venue query. `venues list E` shows evidence and unknowns. `venues compare E` prompts: wheelchair required `yes`; equipment `microphone`.

If needed, `venues add E` provides a local manual fallback. Exact prompt answers for candidate A:

```text
FICTIONAL Demo Indoor Hall A
Fictional planning candidate near York University, Toronto
Fictional demonstration estimate; not a real venue or quote
ESTIMATED
100
200
yes
microphone
yes
```

For candidate B repeat with `FICTIONAL Demo Indoor Hall B`, the same fictional address/source/status, capacity `70`, rental `150`, wheelchair `[blank]`, equipment `[blank]`, save `yes`. Expected: B has a represented capacity shortfall and unknown accessibility/equipment; A's estimates still require verification. Both have unknown availability. Compare with the same needs; explanation requires Gemini. Manual entry is not a provider-research success or booking.

### F09 — Drafting and grounded FAQ (before F08)

`communication generate E` → `EMAIL`, `friendly and concise`. Copy M from the output; `communication list E`, `communication view E M`. `communication edit E M` → enter the exact reviewed editor sentence from GUI F09. `communication approve E M` → `yes` after verifying displayed text. Editing again resets approval; reapprove for F08.

`faq ask E` → `When is the event?`; repeat → `What is the registration URL?`.

Expected: a saved draft then approved text, no sending; grounded saved start time then conservative unknown answer. Generation also accepts `INSTAGRAM` or `WHATSAPP` as prompt values, not extra command arguments.

### F07 — Snapshot and unchanged real event

`event view E`; `scenario simulate E` → `What if attendance increases to 100 and the spending limit becomes CAD 1200?`; `event view E` again.

Expected: explicit simulation-only output, before/after and qualified deltas, but real event still 80/1000. Perform the Section 5 close/reopen check now, before real F08 changes.

### F08 — Structured proposal, cancellation, confirmation, contingency

Run `plan update E`; choose `structured`. The next **eight** prompts are name, type, location, dateTime, endDateTime, attendance, budget, unavailableSpeaker. Respond:

```text
[blank]
[blank]
[blank]
[blank]
[blank]
[blank]
900
[blank]
```

`plan changes E` shows pending fields. `plan confirm E` → `no`; `event view E` must still show 1000. Repeat the proposal, `plan confirm E` → `yes`. Show `event view E`, `plan changes E`, `tasks list E`, `schedule conflicts E`, `budget validate E`, and `communication view E M`.

Expected: real limit 900 only after yes; Observer warnings/rechecks and communication approval reset, existing plans retained. `plan discard E` is the alternative rejection command for a pending proposal. CLI `no` confirmation cancels the proposal, unlike merely closing the GUI confirmation dialog. Natural mode is also supported: `plan update E` → `natural` → proposed-change sentence; it uses Gemini and requires inspection of interpreted fields.

`contingency generate E` → `Catering delay and unsafe outdoor running conditions`; `contingency list E`. Expected: saved unverified recommendations, no automatic application or weather lookup. Repeat structured update with only budget `1000`, confirm `yes`, then review the retained plans. Historical warnings can remain.

### F10 — Record fictional outcomes, analyze, view

Read the fictional-outcomes statement aloud. `tasks list E` to identify approved tasks (discard an unwanted task draft first if necessary). Run `postevent record E`. Prompt answers, in order:

1. Actual attendance `72`; actual expenses `920.00`.
2. Completed approved task IDs: actual comma-separated IDs selected above; unfinished IDs: the remaining approved ID, or blank if none. Sets must be disjoint; omitted tasks are unrecorded, not automatically completed.
3. Organizer feedback: the exact FICTIONAL organizer entry in Section 2, then `[blank]` to finish that list.
4. Attendee feedback: the exact FICTIONAL attendee entry, then `[blank]`.
5. Problems: the exact FICTIONAL problem entry, then `[blank]`.
6. Review the displayed data; save with `yes`.

Then `postevent analyze E` and `postevent view E`. Expected: genuinely generated stored report, arithmetic as Section 2, and missing-data warnings where applicable. Recording actuals is local; analysis uses Gemini. Do not state that this future event actually occurred.

## 5. Persistence checks and expected outcomes

**At F07, before F08:** note event ID, 80 attendance, CAD 1000 limit and approved blueprint ID. Run the simulation, verify unchanged live details, then close normally (GUI window close or CLI `exit`), wait for process completion, and relaunch the same interface with the **same** `PLANIT_DATA_DIR`. Select/view the same event. Expected: original 80/1000 remains saved, approved blueprint/alternatives and plans remain, no persisted hypothetical snapshot. This is stronger evidence than only navigating away from a simulation.

**After all features:** record a short inventory of stable event/blueprint IDs, approved tasks/schedule/budget, a communication and its actual approval flag, recorded outcomes and report count. Close/reopen and inspect those same records. Expected: saved data/flags persist without regeneration or provider calls. Unsaved form text, FAQ display and simulation display do not persist. If demonstrating a draft or pending real proposal, create one deliberately, note its state and verify it survives too; do not confuse a pending proposal with applied facts.

**Cross-interface handoff:** after closing GUI and waiting for exit, set the terminal override to the GUI recording directory and run `mvn compile exec:exec@cli`. `event list`, `event view E`, `blueprint list E`, `tasks list E`, `schedule view E`, `budget view E`, `communication list E`, `postevent view E` should expose the same saved IDs/data. `exit`; reopen GUI and inspect again. Do not execute generation simply to prove loading.

The JSON is saved after successful mutations, not only on shutdown. Storage errors must be shown as failures, not successful saves. There is no automatic backup history. Copy a store only while both interfaces are closed if preparing a known-good recording backup; never overwrite an active store. Do not edit real saved data to create demonstration evidence.

## 6. Limitations and source basis

This guide follows current source over older guide examples. The updated audit is [PLANIT_FINAL_INTEGRATION_AUDIT.md](PLANIT_FINAL_INTEGRATION_AUDIT.md); there is no separate `FINAL_INTEGRATION_AUDIT.md`. Requirements available here are the F01–F10 specifications and design in [STAGE1_REPORT.md](STAGE1_REPORT.md), with Stage 2 implementation/acceptance evidence in the updated audit and milestone guides. No separately named Stage 2 assignment/rubric document was found; this guide cannot certify requirements absent from the repository.

Syntax/startup/UI were checked against [pom.xml](ai-event-planner/pom.xml), [EventPlanningCLI.java](ai-event-planner/src/main/java/com/eventplanner/view/EventPlanningCLI.java), [App.java](ai-event-planner/src/main/java/com/eventplanner/App.java), [CLIApp.java](ai-event-planner/src/main/java/com/eventplanner/CLIApp.java), the current [GUI panels](ai-event-planner/src/main/java/com/eventplanner/view/gui), and [AdaptivePlanningService.java](ai-event-planner/src/main/java/com/eventplanner/service/AdaptivePlanningService.java). See also [persistence](PERSISTENCE_GUIDE.md) and [blueprint selection](F02_BLUEPRINT_SELECTION_GUIDE.md). Older README/milestone statements about lost-on-exit data, deferred F07–F10 or absent venue tools are historical and do not describe the current implementation.

Preserve these additional audit qualifications when narrating:

- Blueprint changes retain execution plans/drafts and flag review; selecting another alternative alone keeps the current execution approval. No automatic semantic reconciliation, unapprove or blueprint deletion/editing exists.
- Formal F08 confirmation notifies Observers; direct member/resource/speaker/window configuration can bypass notifications. Explicitly validate after configuration. Observer task checks omit other-event workload; GUI creates separate per-event member IDs.
- Free-text constraints need human review. AI facts, feasibility, supplier prices/contacts and listings are not independently verified. No booking, publishing, supplier/weather availability feed or automatic contingency application exists.
- Currency changes relabel rather than convert amounts; historical post-event units are incomplete. Keep CAD throughout.
- Saved historical review warnings may remain after successful reapproval. An empty GUI task panel can misleadingly show APPROVED before generation; show actual rows/state rather than relying on that badge.
- Live provider quality, every GUI interaction, timezone handling and full editing scope are not established by the audit's offline test results. Do not describe this document or prior tests as a completed live recording.

## 7. Video timestamps (fill after recording)

Use `mm:ss` or `hh:mm:ss`; leave blank until the footage exists. Note any saved-data-only/provider-failure sections honestly.

| Feature | GUI video timestamp | CLI video timestamp | Recording notes |
| --- | --- | --- | --- |
| F01 Idea fusion | | | |
| F02 Blueprints / selection / approval | | | |
| F03 Tasks / deadlines | | | |
| F04 Schedule / conflicts | | | |
| F05 Budget / catering | | | |
| F06 Venue research / comparison | | | |
| F07 What-if / unchanged original | | | |
| F08 Proposal / cancellation / confirmation / Observer | | | |
| F09 Communications / grounded and unknown FAQ | | | |
| F10 Fictional post-event analysis | | | |
| Persistence / cross-interface handoff | | | |
