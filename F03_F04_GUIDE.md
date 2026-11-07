# F03 tasks and F04 schedules

Run from the application directory so the existing Gemini loader finds `.env`:

```powershell
cd ai-event-planner
mvn compile exec:java
```

Use `help` to see all commands. IDs in angle brackets below are placeholders;
replace them with IDs printed by the CLI. Existing event, idea, concept and
blueprint commands work as before. F03/F04 include available blueprint agenda and
checklist data in their AI context; if there are no blueprints, they use event
details, ideas and constraints. Only `tasks generate` and `schedule build` call
Gemini; all input, review, editing, validation and approval commands are local.

## Enter known planning data

| Command | Interactive input and behavior |
| --- | --- |
| `team set <eventId>` | Stable member ID, name, comma-separated skills/preferences, availability windows, existing workload hours, total capacity hours. Reusing an ID replaces that event's member profile. |
| `team list <eventId>` | Shows member IDs, skills, preferences, availability, workload and capacity. |
| `speaker set <eventId>` | Stable speaker ID, name and availability windows; reusing an ID replaces the profile. |
| `resource set <eventId>` | Stable space or exclusive resource ID and availability windows; reusing an ID replaces its windows. Enter each room and limited resource such as `main-room` or `projector-1`. |
| `schedule window <eventId>` | Shows the existing event start and asks for an event end time. Setup and teardown must also fit inside this window. |

Dates use ISO local datetime strings, such as `2099-01-10T09:00`. Use the same
local timezone for every time associated with an event. Availability input asks
whether to add a window; type `yes`, enter start/end, and repeat as necessary.
Any answer other than `yes` ends the list. An empty availability list means
**unknown**, and cannot establish a valid assignment or resource booking.

Task workload is measured in hours over the planner's chosen planning horizon.
Existing workload means work **outside** the event tasks already stored in this
application. Validation adds all active assigned tasks from the proposed plan and
other stored events, identified by the same stable member ID. Do not include
those tasks a second time in the existing-workload input. `DONE` tasks release
their estimated workload. Capacity is total hours, not remaining hours.

## Task commands

| Command | Behavior |
| --- | --- |
| `tasks generate <eventId>` | Requests at least two structured, context-specific tasks; displays IDs, descriptions, assignees, work windows, deadlines, effort, required skills, priorities, status and warnings. Type `yes` at the approval prompt to validate and save; otherwise retain a draft. Repeating generation replaces only the draft. |
| `tasks list <eventId>` | Shows the draft if one exists, otherwise approved tasks, with current validation warnings and overdue deadlines. |
| `tasks assign <eventId> <taskId>` | Lists known member IDs and asks for assignee (blank means unassigned), work start and deadline. Displays the revised draft and asks whether to approve it. |
| `tasks status <eventId> <taskId>` | Asks for `TODO`, `IN_PROGRESS` or `DONE`, displays the draft, and asks whether to approve. |
| `tasks approve <eventId>` | Revalidates the entire current draft and stores it on the existing Event if valid. |
| `tasks discard <eventId>` | Discards the task draft and restores the view of the last approved task plan. |

Example review sequence:

```text
tasks generate <eventId>
# At the approval prompt, answer no to review or change assignments.
tasks list <eventId>
tasks assign <eventId> <taskId>
# Enter the member ID (or leave blank), work start and deadline.
# Answer no if further revisions are needed.
tasks status <eventId> <taskId>
# Choose TODO / IN_PROGRESS / DONE and explicitly approve if ready.
tasks approve <eventId>
```

AI-suggested unknown, unskilled, unavailable or overloaded assignees are cleared
to unassigned, with their original validation warnings retained for review.
No fictional member is added. An unassigned task can be explicitly approved but
remains visibly unassigned. Manual invalid assignments remain drafts and block
approval. A task's required skills must all match known member skills, ignoring
case; preferences guide AI suggestions rather than acting as mandatory skills.
The full work window must fit a recorded member availability window. Estimated
effort must fit that window. Active deadlines cannot already have passed, and all
deadlines must be no later than the event start. Completed tasks retain their
historical deadlines; skill, identity and availability checks still apply.

## Schedule commands

| Command | Behavior |
| --- | --- |
| `schedule build <eventId>` | Requires an explicit event end time; requests a structured agenda containing setup, activities, breaks and teardown. Displays session IDs and deterministic conflicts, then asks whether to approve. Answering no retains a draft. Repeating generation replaces only the draft. |
| `schedule view <eventId>` | Shows draft or approved sessions chronologically, including time, type, speaker, space, resources and current conflicts. |
| `schedule conflicts <eventId>` | Rechecks the visible schedule against current speakers, resources and event boundaries. |
| `schedule edit <eventId> <sessionId>` | Shows the current session, then asks for title, kind, start/end, speaker ID (blank means none), space ID and comma-separated exclusive resources (blank means none). Displays the revised draft and asks whether to approve. |
| `schedule approve <eventId>` | Revalidates the complete draft and saves it on the Event only if there are no deterministic conflicts or unknown required availability. |
| `schedule discard <eventId>` | Discards the schedule draft and restores the view of the approved schedule. |

Example:

```text
schedule window <eventId>
resource set <eventId>
speaker set <eventId>
schedule build <eventId>
# Answer no to keep the proposal for review.
schedule view <eventId>
schedule conflicts <eventId>
schedule edit <eventId> <sessionId>
# Enter replacement session fields, then answer no for further review.
schedule approve <eventId>
```

All spaces and listed resources are exclusive, with case-insensitive resource
IDs. Two simultaneous sessions in independent rooms with independent resources
and speakers are allowed. Reusing a speaker, room or resource is a conflict only
when the intervals actually overlap. Intervals are half-open, so a session ending
at 11:00 and another starting at 11:00 do not overlap. Setup must precede all
activities/breaks and teardown must follow them. Availability must cover the
entire booking within a recorded window. Unknown speakers, unknown required
availability, unavailable resources and out-of-bounds sessions block approval.
Staff needed for sessions can be entered as exclusive resource IDs with their
confirmed availability, as rooms and equipment are.

Configuration changes never silently rewrite approved plans. Use `tasks list`
and `schedule conflicts` after changing member, speaker, resource or timing data.
Approval always checks the current data again. Free-text constraints, actual
venue capacity, prices and external availability still require planner review;
these services validate only represented facts. Availability and schedule checks
are per event; cross-event speaker/resource reservation management is not modeled.

## Architecture and Stage 1 mapping

The flow remains CLI → Controller → Facade → EventPlanningAgent / Services.
The agent uses the existing `AIModel` abstraction and Gemini adapter to request
JSON. Duplicate JSON keys, trailing tokens, missing/wrong-typed fields, invalid
dates/enums, invalid durations and incomplete agenda structures are rejected.
Malformed output is retried at most once. Transport failures are not retried as
malformed output. Deterministic infeasibility is shown for planner revision,
rather than hidden in another generation attempt. Failed generation leaves
previous drafts and approved plans intact.

New production files, relative to `ai-event-planner/src/main/java/com/eventplanner/`:

| File | Stage 1 correspondence |
| --- | --- |
| `model/Task.java` | F03 / UC03 / SD02 Task, with ID, title, description, optional assignee, deadline, priority and status; immutable revision methods. |
| `model/TimeSlot.java` | F04 TimeSlot and deterministic `overlaps()` / containment operations. |
| `model/Speaker.java` | F04 Speaker identity, name, availability and `checkAvailability()`. |
| `model/Session.java` | F04 Session identity, title, kind, time slot, optional speaker, space and limited resources. |
| `model/Schedule.java` | F04 Schedule containing immutable Session snapshots with unique IDs. |
| `service/TaskService.java` | Stage 1 `validateAssignments()` and `saveAssignments()`, plus deadline and unassigned warnings. |
| `service/ScheduleService.java` | Stage 1 `detectConflicts()` and `saveSchedule()`; deterministic overlap, availability and boundary checks. |

Existing `User` gains skills, preferences, availability, external workload and
capacity. Existing `PlanningTeam` enforces unique member IDs. Existing `Event`
owns its team, approved tasks and approved schedule, plus speakers, event end and
resource availability. Existing agent, facade, controller and CLI gain the
operations described above. Existing EventService remains the event repository;
approved changes mutate the same Event object retrieved from it. Drafts live in
the facade and are cleaned up when discarded, approved or the event is deleted.

Necessary refinements to the Stage 1 design:

- Stage 1 did not define task effort units or concrete workload limits. Tasks add
  `workStart`, `effortHours` and `requiredSkills`; members add total capacity hours
  to make skills, deadline feasibility and overload checks deterministic.
- The starter Event had a start datetime but no end. An explicit end and a
  resource-availability registry are required to check agenda bounds and bookings.
- The diagram's direct Task→User and Session→Speaker references are implemented
  as stable IDs resolved against the event's known members/speakers. Null means
  unassigned/no speaker. Speaker contact details are not needed for this milestone.
- Task, Schedule and Session use immutable snapshots, consistent with the existing
  F01/F02 records, instead of the diagram's in-place mutation methods. Revisions
  create a draft and replace the approved snapshot only after validation.
- TimeSlot uses two LocalDateTime values instead of separate date/time fields;
  this also represents windows spanning midnight. No timezone conversion is implied.
- Agenda periods have explicit kinds; resources currently have unit capacity.
  Larger capacities can be represented as distinct named units. Multi-capacity
  reservations and cross-event resource/speaker calendars are outside this milestone.
- Stage 1's planner approval step is explicit: drafts are separate, rejected
  approval cannot overwrite an approved plan, and unknown required availability
  blocks approval. No Observer/F08 or new JavaFX screens are introduced here.

Storage remains in memory, as in the existing application; exiting loses events,
profiles, drafts and approved plans. The JavaFX starter, dependencies and Gemini
credential configuration are unchanged.

## Tests

```powershell
cd ai-event-planner
mvn test
```

New files under `src/test/java/com/eventplanner/`:

| File | Coverage |
| --- | --- |
| `support/ExecutionFixtures.java` | Synthetic event, member, task, agenda and structured JSON fixtures only. |
| `service/TaskServiceTest.java` | Valid/unassigned tasks; skills, availability, deadline, effort, workload, completed work and immutable approved storage. |
| `service/ScheduleServiceTest.java` | Valid agendas, actual overlaps, adjacent intervals, independent rooms, unavailable/unknown speakers/resources, ordering and bounds. |
| `agent/ExecutionAgentTest.java` | Fake-AI context and proposals; required schema fields, types, dates, enums, retry limits and transport errors. |
| `facade/ExecutionWorkflowTest.java` | Controller/facade draft, approval, revision, revalidation, cross-event workload and failed-generation behavior. |
| `view/ExecutionCLITest.java` | All seven required commands, supporting inputs, manual revisions, approval rejection, input retries and EOF. |

Regular tests use fake AIModel implementations and make no Gemini calls. The
pre-existing real-API smoke test remains opt-in and skipped by `mvn test`.
