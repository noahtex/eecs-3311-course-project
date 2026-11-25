# Planit — AI Event Planning Agent

**EECS 3311: Software Design · York University · Fall 2026 · Stage 2**

Planit is an implemented Java/JavaFX event-planning application for university clubs and small teams. Its graphical dashboard and command-line interface share the same controller, services and persistent local JSON store. Gemini generates planning proposals; Java services check represented constraints, calculate totals and manage approvals. Organizers review proposals and confirm real changes.

[GitHub repository](https://github.com/noahtex/eecs-3311-course-project) · [Demonstration guide](DEMONSTRATION_GUIDE.md) · [Stage 2 UML](diagrams/stage2/README.md)

## Features

All ten features are available through both GUI and CLI.

| ID | Feature | Implemented behavior |
| --- | --- | --- |
| F01 | Collaborative idea fusion | Combine attributed organizer ideas into concepts with source references, conflicts, compromises and uncertainties. |
| F02 | Blueprints and feasibility | Generate alternatives using budget, community or attendance priorities; inspect warnings, select an alternative and explicitly approve it for execution. |
| F03 | Task assignment and deadlines | Propose assignments using member profiles; check skills, availability, workload and deadlines; revise, approve or discard drafts. |
| F04 | Agenda and conflict detection | Generate sessions and check event bounds, timing, speakers and resources; revise sessions before approving the schedule. |
| F05 | Budget and catering | Propose categorized allocations and catering with provider context; edit allocations, calculate totals and approve reviewed estimates. Unknown costs and over-limit budgets block approval. |
| F06 | Venue research and comparison | Discover Google Places candidates or add manual candidates; preserve evidence and unknowns, score fit and generate comparison explanations. |
| F07 | What-if simulation | Analyze hypothetical changes on an isolated snapshot without changing the saved event; show impacts and qualified estimates. |
| F08 | Adaptive updates and contingencies | Preview real changes, confirm or discard proposals, flag retained plans for review and generate contingency recommendations. |
| F09 | Communications and FAQ | Generate, edit and approve email/Instagram/WhatsApp drafts; answer a narrow set of name/type/start-time questions from saved facts. |
| F10 | Post-event analysis | Record actuals, task outcomes and feedback; calculate variances against the current approved plan and generate qualitative lessons and recommendations. |

## Technology and design

The [Maven configuration](ai-event-planner/pom.xml) targets **Java 21** and pins **JavaFX 21.0.9**, **Google GenAI Java SDK 1.80.0**, **Jackson databind 2.18.11** and **JUnit Jupiter 5.11.4**. Google Places API (New) supplies venue and catering-provider discovery. Jackson handles structured AI responses and local JSON persistence.

| Pattern | Participating implementation |
| --- | --- |
| MVC | `Dashboard`/feature panels and `EventPlanningCLI` → `EventPlanningController` → domain models/services. |
| Facade | `EventPlanningFacade` coordinates planning, validation, approvals, tools and persistence. |
| Strategy | `PlanningStrategy` with `BudgetPriorityStrategy`, `CommunityPriorityStrategy` and `AttendancePriorityStrategy` supplies interchangeable prompt guidance. |
| Observer | `Event.applyChanges` notifies `TaskService`, `ScheduleService` and `BudgetService` through `EventObserver` during formal real-change confirmation. |
| Adapter | `GeminiAdapter` implements `AIModel` and delegates to `GeminiAPIClient`, keeping the agent independent of SDK calls. |

See the [Stage 2 class, use-case and sequence diagrams](diagrams/stage2/README.md) and [UML verification](UML_VERIFICATION.md) for relationships and scope.

## Standalone terminal JAR

### Download the standalone CLI

A prebuilt executable version of Planit's command-line agent is available from the [GitHub Releases page](https://github.com/noahtex/eecs-3311-course-project/releases/tag/v1.0.0-stage2).

Download `ai-event-planner-1.0-SNAPSHOT-cli.jar` from the latest applicable release. Java 21 or newer is required; Maven is not needed to run the downloaded JAR.

```powershell
java -jar ai-event-planner-1.0-SNAPSHOT-cli.jar --help
java -jar ai-event-planner-1.0-SNAPSHOT-cli.jar
java -jar ai-event-planner-1.0-SNAPSHOT-cli.jar --llm gemini --input task.txt
```

The task-file example requires a previously saved event, a valid event ID in `task.txt`, and a locally configured Gemini API key. See the configuration and task-file instructions below.

Build once from the `ai-event-planner` directory with JDK 21 or newer and Maven:

```powershell
mvn clean package
```

The executable is `ai-event-planner/target/ai-event-planner-1.0-SNAPSHOT-cli.jar` relative to the repository root. Copy this JAR to another machine with Java 21 or newer; Maven is not needed to run it. The normal GUI (`mvn javafx:run`) and Maven CLI (`mvn exec:exec@cli`) remain available. The packaged JAR starts the CLI, not the GUI.

From any directory, quote paths that contain spaces:

```powershell
java -jar "C:\path with spaces\ai-event-planner-1.0-SNAPSHOT-cli.jar" --help
java -jar "C:\path with spaces\ai-event-planner-1.0-SNAPSHOT-cli.jar"
java -jar "C:\path with spaces\ai-event-planner-1.0-SNAPSHOT-cli.jar" --llm gemini --input "C:\path with spaces\task.txt"
```

Without arguments, the interactive CLI starts; type `help` or `exit`. `--help` prints usage. File mode requires both `--llm gemini` and `--input PATH`, in either order. Gemini is the only supported LLM selection. Configure `GEMINI_API_KEY` and optionally `GEMINI_MODEL` in the process environment or in a `.env` file in the **current working directory**, not necessarily beside the JAR or task file. Environment variables override `.env`; the default model is `gemini-flash-latest`. Do not put credentials in arguments or task files. AI requests require network access, an authorized Gemini model and sufficient quota; event data is sent to Gemini.

The UTF-8 plain-text task format contains one `key=value` per line; blank lines and lines beginning with `#` are ignored. The included [task.txt](ai-event-planner/task.txt) is a template:

```text
action=fuse-ideas
eventId=REPLACE_WITH_SAVED_EVENT_ID
```

First use the interactive CLI to create an event (`event create`), add organizer ideas (`ideas add <eventId>`), and find its ID with `event list`. Replace the placeholder with that ID. Running the task invokes Planit's existing collaborative idea fusion. On success it prints `Saved 2 generated concepts for event ...` (the count can vary), followed by titles, summaries, themes, source idea IDs, conflicts, compromises and uncertainties. Generated proposals are unverified and require organizer review; file mode does not approve a blueprint. This mode currently supports only fusion for an existing event, not free-form instructions or creation of events.

File mode opens the same local JSON store as the GUI and interactive CLI, takes its exclusive session lock, and persists successful fusion results to that event. Close other Planit sessions before running it. On Windows the default is `%LOCALAPPDATA%\Planit`; on macOS it is `~/Library/Application Support/Planit`; on Linux it is `$XDG_DATA_HOME/planit` or `~/.local/share/planit`. Set `PLANIT_DATA_DIR` to select a separate store, or pass `-Dplanit.dataDir=...` before `-jar`. Saved concepts can be inspected through `concepts list <eventId>`; existing events remain available. Exit codes are `0` success, `2` invalid arguments or task contents, `3` unreadable or missing input file, `4` missing Gemini key, and `5` configuration, storage or workflow failure. Authentication rejection, quota and network failures are operation failures (5).

## Windows setup

### 1. Install prerequisites

**To run the standalone CLI:** Install Java 21 or newer. Maven and Git are not required when using the prebuilt JAR from GitHub Releases.

**To build from source or run the JavaFX GUI:** Install JDK 21 or newer, Apache Maven, and Git. Ensure the required tools are available on your system's PATH and that `JAVA_HOME` points to your JDK installation.

```powershell
java -version
mvn -version
git --version
```

A desktop session is required for JavaFX. Internet access is needed for initial dependency downloads and live provider requests.

### 2. Open the project

For a fresh checkout:

```powershell
git clone https://github.com/noahtex/eecs-3311-course-project.git
cd eecs-3311-course-project
cd ai-event-planner
```

If you already have the repository, open PowerShell at its root and run only:

```powershell
cd ai-event-planner
```

All Maven commands below run from **`ai-event-planner`**, where `pom.xml` is located.

### 3. Configure providers locally

Copy the tracked example without overwriting an existing configuration:

```powershell
if (-not (Test-Path -LiteralPath .env)) { Copy-Item -LiteralPath .env.example -Destination .env }
git check-ignore .env
```

Privately edit **`ai-event-planner/.env`** using the names in [`.env.example`](ai-event-planner/.env.example):

| Variable | Purpose |
| --- | --- |
| `GEMINI_API_KEY` | Your Gemini API credential; required for AI generation. |
| `GEMINI_MODEL` | Model available to your account. The template and code default to `gemini-flash-latest`; availability must be confirmed for your account. |
| `GOOGLE_PLACES_API_KEY` | Separate Google Maps Platform credential for live venue/provider discovery. Enable Places API (New) and applicable billing. |

Replace the template's blank/placeholder credentials locally. Never commit, print or share real keys. You may instead configure these variables in the launch process's environment. Environment values take precedence over `.env`, including blank values; restart the application after changes. The loaders read `.env` from the current working directory.

Ordinary local event operations and offline tests need no provider credentials. Full AI workflows require Gemini; live discovery requires Places. Venue candidates can also be entered manually. Provider requests send relevant planning/search data externally and may consume quota or incur charges.

### 4. Test and launch

Run the offline suite:

```powershell
mvn test
```

Launch the GUI:

```powershell
mvn javafx:run
```

Close the GUI and wait for its process to exit before launching the CLI against the same store:

```powershell
mvn compile exec:exec@cli
```

The GUI starts `com.eventplanner.App`; the separate CLI JVM starts `com.eventplanner.CLIApp`. Use `help` at the `planner>` prompt and `exit` to close the CLI. See [CLI lifecycle notes](CLI_LIFECYCLE.md) for the preferred launcher and legacy `exec:java` caveats.

## Saved data

Successful meaningful changes save automatically to **`%LOCALAPPDATA%\Planit\planit.json`** on Windows, with a fallback under `user.home\AppData\Local\Planit`. Saved events, generated alternatives, drafts, approvals, pending changes and reports survive restarts. Simulations, FAQ displays and unsaved form text are transient.

**Do not run the GUI and CLI simultaneously against the same data directory.** An exclusive OS lock rejects a second owning session. Close one interface before opening the other; do not delete `planit.lock` to bypass locking.

To use a separate store, set this before either launcher in PowerShell:

```powershell
$env:PLANIT_DATA_DIR = Join-Path $env:LOCALAPPDATA 'PlanitDemo'
```

Override precedence is the **application JVM's** `planit.dataDir` system property, then `PLANIT_DATA_DIR`, then the platform default. `PLANIT_DATA_DIR` is an environment override, not a `.env` setting. A Maven `-Dplanit.dataDir=...` option is not reliably forwarded to every child JVM; use the environment override with the documented launch commands. Keep custom data directories outside the repository. Storage is local plaintext JSON with no automatic backup history. See the [persistence guide](PERSISTENCE_GUIDE.md) for validation, conflicting writers and recovery.

## CLI quick workflow

Enter these commands at `planner>`. Replace angle-bracket placeholders with IDs actually printed by Planit; supply requested text and configuration at the subsequent prompts.

```text
help
event create
event list
constraints add <eventId>
ideas add <eventId>
ideas add <eventId>
ideas fuse <eventId>
concepts list <eventId>
blueprint generate <eventId> <conceptId>
blueprint list <eventId>
blueprint view <eventId> <blueprintId>
blueprint select <eventId> <blueprintId>
blueprint approve <eventId>
```

Use positive attendance for AI planning. Blueprint generation prompts for `budget`, `community` or `attendance`; approval displays the selected alternative and requires `yes`. Selection alone keeps the prior execution approval active. F03/F04 generation uses only the approved blueprint when alternatives exist; events without blueprints retain a labelled event-details-only fallback. Changing approval retains existing tasks/schedules and flags them for review.

Continue with `team set <eventId>` and `tasks generate <eventId>`, or configure availability using `schedule window <eventId>`, `speaker set <eventId>` and `resource set <eventId>` before `schedule build <eventId>`. Tasks, schedules and budgets have separate approval/discard commands. Use `help` and the detailed guides for complete inputs; generated IDs and provider results should be reviewed interactively.

## Verification

Latest verified ordinary offline run, **November 16, 2026 (America/Toronto)**: `mvn test -B -ntp` completed with **BUILD SUCCESS — 175 reported, 160 passed, 15 skipped, 0 failures, 0 errors**.

The skips were 14 opt-in JavaFX desktop tests and one opt-in live Gemini test. Injected model/provider responses were used; **no live Gemini or Places tests were run in that execution**. This was an incremental test run, not a clean package or verification of every GUI interaction. See [final report evidence](FINAL_REPORT_EVIDENCE.md) and the [integration audit](PLANIT_FINAL_INTEGRATION_AUDIT.md) for scope and historical desktop results.

## Demonstrations

- [GUI demonstration](https://youtu.be/nZuiaAbg7p8)
- [CLI demonstration](https://youtu.be/z9DXe4JFFQE)

The [demonstration guide](DEMONSTRATION_GUIDE.md) documents commands and workflows; the [evidence index](FINAL_REPORT_EVIDENCE.md) provides fields for feature timestamps.

## Important limitations

- AI proposals, feasibility claims, prices, provider contacts and listings require organizer verification. Venue discovery does not book a venue; contingencies do not fetch weather or apply themselves automatically.
- FAQ answers cover narrow name/type/start-time intents. Communications are drafts for review, with no direct sending or platform publishing.
- Event times have no stored timezone/DST policy. Currency changes do not convert amounts; historical actuals/reports lack currency identity. Keep currency consistent for comparisons.
- Granular editing is incomplete, including blueprint content, source idea/constraint edits, task/session add/remove, budget allocation add/remove and venue candidate edits.
- Direct configuration changes can bypass Observer notifications. Explicitly validate retained plans after such changes; blueprint approval switches require manual reconciliation, and historical review warnings can remain after reapproval.

## Documentation and submission

- **Final report: [Stage 2 Implementation Report](STAGE2_REPORT.pdf)**
- [Feature mappings, test/commit evidence and submission checklist](FINAL_REPORT_EVIDENCE.md) · [Integration audit](PLANIT_FINAL_INTEGRATION_AUDIT.md)
- [Stage 2 UML index: editable sources, SVG and PNG](diagrams/stage2/README.md) · [UML verification](UML_VERIFICATION.md) · [Stage 1 specification/design](STAGE1_REPORT.md)
- Detailed workflows: [F02 blueprint approval](F02_BLUEPRINT_SELECTION_GUIDE.md), [F03–F04 tasks/scheduling](F03_F04_GUIDE.md), [F05–F06 budget/venues](F05_F06_GUIDE.md), [F07–F10 advanced planning](MILESTONE4_GUIDE.md)
- GUI details: [planning panels](MILESTONE5_PART_A_GUIDE.md), [advanced workflows](MILESTONE5_PART_B_GUIDE.md) · [Persistence](PERSISTENCE_GUIDE.md)

Earlier guides retain historical milestone/test notes; use the dated evidence and current source when assessing the final implementation.
