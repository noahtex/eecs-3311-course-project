# JavaFX usability redesign

Launch from `ai-event-planner` with `mvn javafx:run`.

## Design and shared components

The existing Java-built dashboard and all F01–F10 routes remain. The starter FXML files are unused and were left alone. The desktop now uses a light slate/white palette, `#4F46E5` indigo actions, Segoe UI, consistent spacing, rounded cards, readable tables, wrapping paragraphs, status badges, distinct secondary/destructive buttons, styled dialogs and a scrollable sidebar. Header actions wrap, content remains scrollable, and the initial window fits the primary screen's usable bounds, including display scaling. There are no added dependencies, distributed fonts, WebViews or decorative images.

`Ui` supplies reusable field labels, cards, metric tiles, badges, prose/list sections, structured inspectors and collapsed technical panels. `ResultViews` renders domain fields directly instead of parsing CLI output. `Dashboard` remembers per-event selections, priorities, venue search text and page scroll positions alongside the existing Part B input state.

## Date and time controls

| Workflow | Updated inputs |
| --- | --- |
| Create Event | Start and end date/time |
| Tasks | Work start and deadline |
| Schedule | Event end, session start/end |
| Team, speakers and resources | Multiple availability intervals, each with start/end controls |

`DateTimeInput` combines a DatePicker with 24-hour and minute selectors and a human-readable preview. DatePicker supports keyboard entry in its displayed local format. Required date/hour/minute values and invalid typed dates produce a form error. Backend `TimeSlot` validation continues to reject equal/reversed intervals. Existing seconds and nanoseconds survive edits and remain visible in the preview when present. Values serialize to the same ISO `LocalDateTime` strings consumed by the existing GUI session/controller calls. No host timezone, UTC conversion or daylight-saving reinterpretation is introduced. An empty availability collection still means UNKNOWN.

`AvailabilityInput` replaces manual start/end text lists with add/remove interval controls. Inputs remain inside the existing modal forms, with validation errors shown in place.

## Names and identity

- Event selection shows event name, local start and location rather than UUIDs.
- Team tables and assignment selectors show member names; agenda tables and session editors show speaker names.
- Existing speaker profiles/resources can be selected instead of typing their references. New members/speakers receive UUIDs internally. Resource names remain the existing resource identifiers, with friendly fallback labels when only an opaque identifier exists.
- Concept choices show titles. UUID columns were removed from idea, concept and blueprint tables. Communications are labeled by sequence, channel, tone and approval state.
- Post-event task outcomes use task-title selectors with UNRECORDED, COMPLETED and UNFINISHED choices instead of typed task UUIDs.
- Duplicate labels receive stable ordinal context within their entity list. Unknown people use honest fallbacks such as Unknown member / Unknown speaker. Unresolved UUIDs in review text use an unresolved-reference label; exact source text remains inspectable.

`DisplayNames` is a presentation lookup. Selector options retain original IDs; no domain identity, backend association or persistence representation is changed. Existing missing references remain selectable, so opening a form cannot silently reassign or clear one. UUIDs and exact source content remain available in collapsed technical panels.

## Generated results

| Feature | Presentation |
| --- | --- |
| F01 ideas/concepts | Organizer/idea table, titled summaries, themes, conflicts, compromises and uncertainties |
| F02 blueprints | Overview and metrics; distinct agenda, constraints, feasibility, venue/catering assumptions, resources and checklist sections |
| F03 tasks/team | Structured task table, named assignees, readable deadlines, status/priority, effort/skills and availability details |
| F04 schedules | Chronologically ordered sessions, readable local times, named speakers, spaces/resources and conflict review |
| F05 budgets/catering | Spending-limit/subtotal/remaining metrics, line items and price statuses, quote/evidence details, recommendations and provider cards |
| F06 venues | Candidate comparison table, evidence/provenance cards, visibly unknown facts, suitability warnings and trade-offs |
| F07 scenarios | Prominent HYPOTHETICAL status, before/after table, interpreted changes, impact metrics, feasibility and recommendations |
| F08 updates/contingencies | Proposed fields and impact analysis before confirmation; updated event/Observer warnings; risk, impact, response, resources and assumptions |
| F09 communications/FAQ | Editable drafts with separate approval; named draft selection/status; question/answer blocks |
| F10 post-event reports | Planned/actual metrics and variances, task statistics/outcomes, attributed feedback, findings, lessons and missing information |

Original model content, caveats, evidence and warnings are retained. Renderers use wrapping paragraphs and individual sections; they do not reinterpret Markdown or execute HTML. Exact original model content is available in collapsed technical panels, including fields whose display substitutes a friendly name. This provides a safe plain-text fallback for prose without adding a Markdown dependency. No AI prompts or structured response schemas changed.

## Behavior retained

The existing controller/facade architecture, CLI, API adapters, credentials, configuration, models and network cleanup ownership are unchanged. Gemini/Places and validation/approval workflows continue to use the serialized JavaFX Task worker. Progress and duplicate-submission protection remain. Navigation/event selection stays responsive during requests; views reload afterward without rendering mutable domain objects concurrently. Per-event completion storage prevents results from an earlier event appearing under the current selection. Confirmation dialogs and explicit approval rules remain in force.

## Files

Created GUI helpers: `DateTimeInput`, `AvailabilityInput`, `DisplayNames`, `TaskOutcomesInput`, `ResultViews`.

Modified: `Ui`, `Dashboard`, `App`, `dashboard.css`, and the existing Events, Ideas, Blueprints, Tasks, Schedule, Budget, Venues, Scenario, Plan Updates, Communications and Post-Event panel classes.

Tests: added `DisplayNamesTest`; extended `JavaFxSmokeTest` for calendar conversion/creation/validation, original-content preservation, task outcome identity, smaller-window navigation, and the Places research button. Existing GUI async/error/event-isolation/cleanup and F07–F10 workflow tests remain.

## Manual verification

Verified during implementation:

- `mvn test`: BUILD SUCCESS; 142 tests, zero failures/errors, ten skipped (nine desktop tests and the opt-in live Gemini test).
- `mvn '-Dgui.smoke=true' test`: BUILD SUCCESS; 142 tests, zero failures/errors, one skipped (live Gemini).
- `mvn javafx:run`: actual window launched and responded. Its light theme/sidebar/empty state and final screen-sized layout were inspected, including the visible status footer; the window closed normally and Maven exited with BUILD SUCCESS.
- Desktop tests rendered every sidebar page at 1280 and 880 window widths, exercised calendar-based event creation and validation, checked exact original blueprint content, and verified both AI-model and Places-research calls execute off the FX thread using injected test implementations. Existing event-isolation, failure recovery, confirmation and cleanup checks passed.
- `git diff --check`: passed. No staging, commits or pushes were performed.

1. Create an event using the calendars and 24-hour selectors. Try missing dates, nonnumeric attendance/budget and an end that precedes the start. Confirm useful errors and no partial event creation.
2. Add members/speakers with duplicate names and availability intervals. Generate tasks/schedules, edit assignments using names, and verify that the correct person remains associated after approval.
3. Generate each structured result with the configured Gemini/Places integrations. Read all warnings/assumptions and expand technical details to compare original content.
4. Start a real request, resize the window, navigate and change events. Confirm progress continues and the returned result stays with its originating event.
5. Inspect the dashboard and dialogs at different display scales. The sidebar scrolls and the header/content adapt; long tables may scroll horizontally at the smallest supported widths.
6. Record post-event outcomes using task names, leave actual values blank, and generate a report. UNKNOWN must remain distinct from a recorded zero.

Live provider requests, every populated feature at every display scale, and full screen-reader behavior require manual verification. Automated UI tests use injected test responses, never production placeholders. Historical report currency still follows current event configuration because that is the existing backend contract. Events and view state remain in-memory.
