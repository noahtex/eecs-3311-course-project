# Milestone 4: F07-F10

Run from `ai-event-planner`: `mvn exec:java`. Use `event list` for the current session's event IDs. All data is in memory and disappears on exit. No credentials are printed or included in this guide. Existing JavaFX launch and F01-F06 commands remain available.

## CLI reference

Replace `<eventId>` and `<communicationId>` with actual IDs; do not type angle brackets. Complex inputs are prompted interactively.

| Command | Interactive inputs / behavior |
| --- | --- |
| `scenario simulate <eventId>` | Natural-language change, e.g. `Attendance increases from 80 to 150`, `Budget decreases to 700`, `Move to an indoor hall`, or `Speaker sp-1 is unavailable`. Prints before/after fields, impacts, spending-limit delta, estimated expense delta, scheduling/resource implications, warnings and adjustments. Never saves the scenario. |
| `plan update <eventId>` | Choose `natural` or `structured`. Natural input uses Gemini interpretation. Structured mode prompts name, type, location, start, end, attendance, budget and unavailable speaker ID; blank means unchanged. Shows interpreted fields and affected plans; does not apply changes. |
| `plan changes <eventId>` | Shows pending before/after proposal and stored review warnings. |
| `plan confirm <eventId>` | Shows proposal and asks for `yes`. Every other answer cancels and removes it. Confirmed changes update the current in-memory event. |
| `plan discard <eventId>` | Discards pending changes. |
| `contingency generate <eventId>` | Risk description, e.g. `Main speaker cancels`, `Bad weather`, `Venue loses power`, `Catering is 45 minutes late`. Stores a recommendation, not an approved replacement plan. |
| `contingency list <eventId>` | Displays stored risks, impacts, responses, resources and assumptions. |
| `communication generate <eventId>` | Channel `EMAIL`, `INSTAGRAM` or `WHATSAPP`, then tone such as `formal`, `friendly` or `brief`. Returns a separate draft and its ID. |
| `communication list <eventId>` | Displays communications including text and approval status. |
| `communication view <eventId> <communicationId>` | Displays draft text and source facts. |
| `communication edit <eventId> <communicationId>` | Enter replacement text on one line; edit resets approval. Planner is responsible for verifying manually entered factual claims. |
| `communication approve <eventId> <communicationId>` | Displays exact text and asks for `yes` after verifying facts. Does not send or publish. |
| `faq ask <eventId>` | Ask e.g. `When?`, `What is the event name?`, `What type of event?`. Unknown or compound questions return an explicit unconfirmed-information response. |
| `postevent record <eventId>` | Actual attendance, total expenses in event currency (blank = unknown), completed and unfinished approved task IDs, then organizer feedback, attendee feedback and problems as separate entries. Blank ends each feedback list. Confirm `yes` to store. |
| `postevent analyze <eventId>` | Builds and stores a report from recorded outcomes and the current approved plan. |
| `postevent view <eventId>` | Displays all stored reports, including raw feedback perspectives, planned/actual metrics and missing information. |

Example structured update: select `structured`, leave name/type blank, enter `Indoor hall` as location, leave dates blank, enter `150` attendance and `700` budget, leave speaker blank. Review with `plan changes`, then `plan confirm` and `yes`. Approved task, schedule and budget records are retained and flagged for review. Use the existing generation/edit/approval commands to revise them.

## Classes and boundaries

- `AdaptivePlanningService` coordinates F07-F10 through the existing `EventPlanningAgent`, which exposes `milestoneOutput` using the existing AIModel adapter, strict JSON parser and bounded transport/JSON retry. Schema fields map explicitly into domain records. Missing/invalid structured fields are rejected without saving results.
- `PlanChange` captures the original field state and proposed values. Changes are validated on a candidate before atomic assignment; stale proposals cannot overwrite newer core event facts.
- `ScenarioResult` stores proposed fields, a separate `Event` snapshot, affected components, budget differences, implications, warnings and recommendations. Snapshot containers and mutable team members are independent; deeply immutable records may safely be shared. No scenario is placed in EventService or approved draft maps.
- `EventObserver` defines actual change notifications. `Event` publishes after confirmed changes. `TaskService`, `ScheduleService`, and `BudgetService` subscribe through the facade before proposals/confirmation. Each reruns its existing validator against the retained approved component, stores diagnostics, and flags it for review even where numeric validation finds no conflict. No observer replaces approved plans. Existing communications revert to drafts when event facts change.
- `ContingencyPlan` represents risk, impact, response, resources and explicit assumptions. Weather is not retrieved or fabricated.
- `Communication` stores channel, tone, editable text, approval status and source-fact snapshot separately from event facts. Gemini generates constrained nonfactual opening/closing wording and selects fact ordering; Java validates the vocabulary and renders the actual approved values. Invalid fact keys or unsupported wording fail safely. Reapproval after source facts change requires regeneration.
- `PostEventData` records nullable actual metrics and separate feedback perspectives; task outcomes must be unique, disjoint approved IDs. `PostEventReport` stores raw outcomes, expected attendance, planned expense baseline, Java-calculated differences/counts/rate, qualitative analysis and missing information. Reports remain attached to their event, including earlier reports after new outcomes are recorded.
- Controller/facade gain typed delegation methods; CLI adds prompts, displays and help. `module-info.java` opens the domain package only to Jackson for record serialization; date/time serialization is explicit. Maven dependencies and the JavaFX entry point are unchanged.

## Deterministic calculations

Spending-limit difference = hypothetical spending limit minus original spending limit. Estimated expense difference = approved known expense subtotal times hypothetical attendance / original attendance, rounded to two decimals, minus original subtotal. This deliberately simple sensitivity estimate assumes every allocation scales with attendance; it is labelled an assumption, not a revised supplier quote. It is unknown when attendance baseline or complete approved budget is unavailable. Original budget allocations are retained for validation. Existing task, schedule and budget validators run on the scenario.

Post-event attendance difference = actual minus expected. Expense difference = actual expenses minus approved planned expense subtotal in matching currency; no approved budget or unknown amounts produce an unknown difference. Spending limit is not substituted for planned expense. Completion rate = explicitly recorded completed tasks / total approved tasks times 100; unknown task outcomes are reported separately and are not automatically classified as unfinished. With no approved tasks, rate is unknown.

## Stage 1 documentation changes and limitations

1. F07 returns `ScenarioResult` rather than saving a revised `EventBlueprint`. Existing blueprints are proposals and there is no blueprint-approval model, so they are explicitly labelled unapproved in AI context.
2. Update orchestration lives in `AdaptivePlanningService`, called through Facade, rather than adding an EventService update method. Event remains the Observer subject; callback signature includes event and changed field names. All core changes conservatively trigger review of existing plans.
3. `WeatherTool` / `WeatherAPIClient` are not implemented. Contingencies state that no forecasts or availability were retrieved.
4. Communications use constrained AI wording plus deterministic approved-fact rendering, rather than unrestricted AI copywriting. Only stored event name, type and date/time are public facts in this implementation. Existing location is a planning location, not proof of venue booking; budget is not ticket price. Venue, links, ticket prices, accessibility, contact information and registration have no approval/provenance fields and are always unconfirmed. Planner text edits require manual factual verification. Nothing sends email or posts to platforms.
5. FAQ is deterministic and intentionally narrow rather than Gemini-generated free-form answers. Unsupported paraphrases and compound questions return unconfirmed instead of guessing.
6. No automatic cross-event learning or persistent storage is introduced. Reports supply reusable lessons for planners within the current session. Previously approved plan revisions still require their existing explicit approvals; warnings are retained as review history.
7. Free-text constraints and qualitative AI recommendations still need human review. Natural-language changes support only the structured fields listed above; ambiguous/unsupported requests require clarification. Simulations provide attendance-based expense sensitivity, not item-level repricing or automatic schedule regeneration. No live API/GUI validation was performed.

## Automated validation

`mvn -f ai-event-planner/pom.xml test -q` runs offline tests. New `MilestoneFourTest` uses fake AIModel responses and external services that fail if called. Coverage includes independent nested simulations, impacts/expense sensitivity, confirmation/cancellation/staleness, invalid dates/values, real Observer diagnostics including speaker removal, approved-fact drafts, edits/approval/invalidation, ungrounded AI rejection, grounded FAQ, actual metrics/missing data, report storage, failures without saves, contingency mapping and a CLI/controller/facade workflow. Existing F01-F06 tests run in the same suite. The live Gemini smoke test remains explicit opt-in and is skipped by default.

Final executed result: **115 tests, 0 failures, 0 errors, 1 skipped** (114 passed). This includes 19 new MilestoneFourTest tests. The skipped test is the opt-in real Gemini smoke test. Maven exited successfully. `git diff --check` also passed. No staging, commit, or push was performed.
