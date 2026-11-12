# Milestone 5, Part A: JavaFX dashboard

## Launch

From `ai-event-planner`, run `mvn javafx:run` with JDK 21. Or, from the repository root, run `mvn -f ai-event-planner/pom.xml javafx:run`.

The existing Gemini and Google Places configuration applies unchanged. Configure your local `.env` following `.env.example`; never commit credentials. Event creation, editing and viewing work without network access. AI generation and venue research require valid configuration and network access. The CLI remains independent: `mvn compile exec:exec@cli` from the Maven project directory.

## Dashboard workflow

Use the left sidebar for Events, Ideas, Blueprints, Tasks & team, Schedule, Budget & catering, and Venue research. The header selects the active event. Each screen reads the same application session. Data is in memory and is lost when the application exits; switching screens does not create a new service or event store. A footer shows progress and safe feedback.

1. **Events:** Create an event with name, type, location, start, end, attendance and spending limit. Times use ISO local date/time, e.g. `2026-12-01T18:30`. Invalid forms stay open with their entered values and an error. Select an event row to inspect details, set it active or delete it with confirmation. Add goals/constraints for the active event.
2. **Ideas (F01):** Add an organizer and idea. View collected ideas, fuse them with Gemini, then inspect each generated concept's ID, themes, conflicts, compromises and uncertainties.
3. **Blueprints (F02):** Choose a generated concept and budget/community/attendance priority. Generate alternatives and select a row to inspect its ID, agenda, warnings, venue/catering assumptions, resources and checklist. Blueprints remain unverified proposals.
4. **Tasks & team (F03):** Add/edit member skills, preferences, availability, existing workload and capacity. Availability is one `start/end` ISO datetime pair per line; blank means unknown. Generate proposed tasks, inspect assignments/deadlines/priority/status, edit assignment/time windows or status, and review warnings. Approve or discard the draft explicitly; edits do not directly replace approved tasks.
5. **Schedule (F04):** Inspect the event start and set the end of the planning window. Configure speakers and resource availability using stable IDs; replacing existing availability requires confirmation. Generate agenda proposals and view sessions chronologically. Edit session details, check conflicts, and approve/discard drafts. The backend blocks approval where required scheduling information or conflict resolution is missing.
6. **Budget & catering (F05):** Configure currency, dietary and catering needs. Select priority, generate a proposal and inspect quantities, unit costs, category totals and CONFIRMED/ESTIMATED/UNKNOWN status. Edit allocations with provenance, recalculate/validate, review remaining funds and catering recommendations, then approve/discard. Unknown amounts remain visibly unknown and are not silently treated as free; approval of an estimate does not turn it into a confirmed quote.
7. **Venue research (F06):** Enter search requirements and use the real Places integration. Inspect candidates, addresses, source/retrieval information and evidence status. Configure equipment/accessibility constraints and compare suitability. Manual venues record a source and PLANNER_CONFIRMED or ESTIMATED evidence; blank attributes remain UNKNOWN. Availability always remains unknown until directly confirmed; no booking is made. A wheelchair entrance is not a full accessibility guarantee. Search warnings remain visible for the event.

## Implementation and changed files

Only GUI integration, tests and documentation changed; no planning service, application controller, facade, CLI or AI adapter was redesigned.

- Modified `src/main/java/com/eventplanner/App.java`: launches the dashboard with one owned facade/controller/session; shuts it down on app exit. Legacy generated FXML switching helper remains for starter-controller compatibility.
- Added `src/main/java/com/eventplanner/view/gui/Dashboard.java`: layout, navigation, active-event selector, progress/error feedback and serialized background request execution.
- Added `GuiSession.java`: shared controller/selection plus narrow event form delegation; validates all form values before event creation to avoid partial records.
- Added `GuiInputs.java`: GUI text parsing for dates, numeric values, availability, booleans and enums. Domain rules still run in existing models/services.
- Added `Ui.java`: reusable layouts, tables, read-only inspectors, confirmation dialogs and input forms that retain invalid input.
- Added feature components `EventsPanel.java`, `IdeasPanel.java`, `BlueprintsPanel.java`, `TasksPanel.java`, `SchedulePanel.java`, `BudgetPanel.java`, `VenuesPanel.java`.
- Added `src/main/resources/com/eventplanner/dashboard.css`: dashboard typography, navigation, cards, tables, buttons, forms and feedback styles.
- Added `src/test/java/com/eventplanner/view/gui/GuiSessionTest.java` and `JavaFxSmokeTest.java`.
- Added this guide and a README link.

The existing module descriptor and JavaFX Maven launcher already support the programmatic JavaFX layouts and remain unchanged. Dynamic feature forms use JavaFX controls directly; no new FXML reflection opens are necessary. Generated primary/secondary FXML files and their controllers remain unused starter artifacts.

## Threading, approval and resource ownership

GUI callbacks delegate through EventPlanningController and EventPlanningFacade. Screens do not instantiate business services. Gemini and Places operations use JavaFX Task on one worker executor. All control reads occur before dispatch and UI updates occur in FX completion handlers. Navigation, active-event selection and content submissions are disabled for the duration of a request, so the mutable in-memory backend has no concurrent GUI writers. Failed requests show safe errors; arbitrary transport exception messages are not displayed.

Draft generation, edits, approvals and discards use the same backend operations as the CLI. Explicit approval dialogs do not bypass deterministic validation. Delete/discard and replacement of speaker/resource profiles require confirmation. Closing a window stops UI callbacks, lets an already-running bounded request finish, then closes the GUI-owned facade on the worker. Navigation never closes shared resources. The CLI owns its own facade lifecycle.

## Verification

Run offline regression tests with `mvn -f ai-event-planner/pom.xml test -q`.

On a desktop with a display, run JavaFX checks with `mvn -f ai-event-planner/pom.xml '-Dgui.smoke=true' test -q` (PowerShell quoting shown). Default runs skip the display-dependent smoke tests. The live Gemini smoke test remains explicit opt-in and is not enabled by these commands.

Final full run with `-Dgui.smoke=true`: **133 tests, 0 failures, 0 errors, 1 skipped** (132 passed). The skipped test is the opt-in live Gemini smoke test. Maven exited successfully.

Executed checks:

- Full default regression suite, including five new GuiSession/input validation tests: passed.
- Three opt-in JavaFX smoke tests: passed. They launched the actual App/stylesheet, closed it normally, constructed all seven feature screens, navigated while retaining the active event, checked the empty-event state, verified off-FX-thread requests and blocked duplicate submissions, and checked safe API failure feedback. External requests are fake or not invoked.
- `mvn -f ai-event-planner/pom.xml javafx:run`: launched the real dashboard. Its window title was verified and the window closed via a normal close request; Maven exited with BUILD SUCCESS.
- `git diff --check`: passed.

## Manual testing still required / limitations

- Live Gemini concept/blueprint/task/schedule/budget generation and Places research/comparison were not invoked during verification. Validate them manually with configured credentials and review the resulting proposals.
- Modal form submission, edit/approve/discard/confirmation sequences for every feature were not comprehensively automated. Tests cover session/input validation, existing backend workflows, screen construction/navigation and background request handling, not complete UI end-to-end workflows.
- Check visual appearance on your display scale and window size; no human visual review or screenshot comparison was performed.
- All event times are local without timezone conversion. No persistent storage is added. Changing event start after creation uses the existing CLI event-change workflow; this GUI schedule section configures the end against the existing start.
- No F07-F10 screens are included in Part A. No communications are sent and no venues booked. Event concepts, blueprints, estimates and candidate venue information retain the backend's existing approval/provenance limitations.
- No files were staged, committed or pushed.
