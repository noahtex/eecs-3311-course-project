# AI Event Planning Agent

An AI-powered event planning system designed to help university clubs and small teams organize events more efficiently.

This project is being developed for **EECS 3311: Software Design** at York University.

**Milestone 3B (F05/F06) implemented:** Budget/catering proposals, deterministic
budget calculations and approval, real Google Places venue/provider discovery,
manual venue entry and comparison are available through the CLI. See the
[F05/F06 command, configuration and design guide](F05_F06_GUIDE.md).

**F03/F04 implemented:** Smart task assignments and deadline tracking, plus agenda
scheduling and deterministic conflict detection, are available through the CLI.
See the [F03/F04 command and design guide](F03_F04_GUIDE.md) for every new command,
interactive inputs, approval/revision workflows, tests and Stage 1 mappings.

## About the Project

Planning events can be difficult for university clubs, especially when executive members are balancing school, work, and other responsibilities. Event planning also involves many connected decisions, such as budgeting, scheduling, venue selection, task assignments, catering, and managing unexpected changes.

The goal of this project is to create an **AI Event Planning Agent** that works alongside an event planning team. Rather than replacing human decision-making, the agent helps organize information, generate recommendations, identify conflicts, and adapt plans when event details change.

## Main Features

The proposed system includes:

- Collaborative idea fusion
- Event blueprint and feasibility generation
- Smart task assignment and deadline tracking
- Agenda scheduling and conflict detection
- Budget and catering planning
- Venue research and comparison
- What-if scenario simulation
- Adaptive plan updates and contingency planning
- Event communications and attendee FAQ generation
- Post-event analysis

## AI Integration

The system is designed to use **Google Gemini** as its primary large language model.

The AI agent will be responsible for tasks such as combining ideas from multiple organizers, generating event plans, reasoning about event constraints, comparing alternatives, and generating contingency plans.

Traditional software components will still handle deterministic operations such as budget calculations, schedule conflict detection, validation, and updating stored event information.

## Design

The project uses object-oriented design and UML to plan the system before implementation.

The current design includes:

- Class Diagram
- Use-Case Diagram
- Sequence Diagrams
- Feature-to-Design Traceability
- Detailed Feature Specifications
- Design Pattern Documentation

The system is designed around five main design patterns:

1. **MVC (Model-View-Controller)**
2. **Facade**
3. **Strategy**
4. **Observer**
5. **Adapter**

## User Interfaces

The completed system is planned to support both:

- **GUI** – A graphical event planning dashboard
- **CLI** – A command-line interface providing access to the same core functionality

Both interfaces will communicate with the same application controller and underlying system logic.

## Project Status

### Stage 2 command-line workflow

Requires JDK 21 or newer and Maven. From the repository root:

```powershell
cd ai-event-planner
mvn test
mvn compile exec:exec@cli
```

The CLI uses the separate `com.eventplanner.CLIApp` entry point. The existing
JavaFX entry point remains available with `mvn javafx:run`.
Use `exec:exec@cli` to launch a separate JVM with explicit application cleanup;
[CLI lifecycle notes](CLI_LIFECYCLE.md) explain the Okio daemon warning that can
still appear with the legacy in-process `exec:java` launcher.

Commands: `help`, `event create`, `event list`, `event view <id>`,
`event delete <id>`, and `exit`. Creation prompts for name, type, location,
date/time (for example `2026-12-01T18:30`), expected attendance, and total budget.
Attendance must be a non-negative integer; budget must be finite and non-negative.
Invalid field values are explained and prompted again. IDs are generated at creation;
copy the ID from the creation response or event list for viewing and deletion.
Deletion requires typing `yes`; other responses cancel it. End-of-input exits
cleanly and cancels any unfinished operation.

Planit now saves GUI and CLI data automatically to a shared **local JSON store**.
On Windows the default is `%LOCALAPPDATA%\Planit\planit.json`. Only one GUI or CLI
session may open the store at a time. See the [persistence guide](PERSISTENCE_GUIDE.md)
for directory overrides, saved draft/approval states, failure handling and recovery.

MVC flow: `EventPlanningCLI` (view) → `EventPlanningController` →
`EventPlanningFacade` → `EventService` → `Event` (model).
The controller delegates application operations, while the facade validates event
details and provides a simple interface over the existing service. `CLIApp` wires
one shared `EventService` instance for the entire session.

### Milestone 2: F01 idea fusion and F02 blueprints

The CLI now uses real Gemini calls for idea fusion and blueprint generation through
the official `com.google.genai:google-genai:1.80.0` SDK. Jackson `2.18.11` parses
structured JSON into immutable domain objects. JavaFX `App.java` and its launcher
remain unchanged; `module-info.java` adds only the SDK and Jackson requirements.
SDK setup follows the [official Google Gen AI Java SDK documentation](https://github.com/googleapis/java-genai).

**Safe local configuration** (run from `ai-event-planner`):

```powershell
# Only when you do not already have .env:
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Edit `.env` locally to set `GEMINI_API_KEY` and `GEMINI_MODEL`. The example contains
no credential. `.env` is ignored by `ai-event-planner/.gitignore`; verify with
`git check-ignore .env` from the project folder. Never commit or print your key.
Alternatively, set these environment variables in your local terminal or IDE.
Environment values take precedence over `.env`, including blank values. The loader
reads `.env` from the current working directory, supports quoted `KEY=value` lines
and optional `export`, and does no interpolation or shell execution. Use full-line
comments rather than inline comments. The model defaults to `gemini-flash-latest`;
choose an available model for your account, or a specific model ID for repeatability.
No key is required for ordinary event commands. Credentials are loaded only on an
AI request. Errors omit SDK response bodies and credentials.

Launch and test:

```powershell
cd ai-event-planner  # from the repository root
mvn test
mvn compile exec:exec@cli
```

In the CLI, use this sequence (replace the angle-bracket placeholders with IDs
printed by the preceding commands):

```text
event create
ideas add <eventId>
ideas add <eventId>
ideas list <eventId>
constraints add <eventId>
constraints list <eventId>
ideas fuse <eventId>
concepts list <eventId>
blueprint generate <eventId> <conceptId>
blueprint list <eventId>
blueprint view <eventId> <blueprintId>
exit
```

`event create` prompts for the existing event fields. `ideas add` prompts for
organizer name and idea. `constraints add` accepts an additional goal or constraint,
such as "step-free access, two volunteers, no alcohol"; repeat to add more.
`ideas fuse` displays at least two concepts, source idea IDs, themes, conflicts,
compromises, and uncertainties. `blueprint generate` asks for `budget`, `community`,
or `attendance`, then stores at least two alternatives. Blueprints include agenda,
venue requirements, catering assumptions, resources, checklist, assumptions, and
feasibility warnings. AI requests send the event details, constraints, organizer names,
ideas, and selected concept to Gemini and consume your API quota.

**Validation and uncertainty:** Planning requires a name, type, location, date/time,
positive expected attendance, and a finite non-negative budget. Event creation still
allows zero attendance for compatibility, but planning asks for a positive target.
Missing ideas or invalid IDs fail before calling Gemini. Numeric budgets, proposed
attendance, and capacity are checked in Java. Over-budget proposals, attendance
mismatches, and capacity shortfalls are retained with explicit conflict warnings and
suggested corrective action. Free-text constraints require manual review; the app
does not claim to verify arbitrary natural-language rules. Unspecified goals,
resources, accessibility and staffing remain unknown.

Generated narratives and costs are **unverified planning proposals**, not venue
availability, actual capacity, vendor prices, or weather facts. The prompt prohibits
claiming those facts, and every result includes an application-added unverified
warning that the model cannot omit. No external venue/catering/weather tools are
implemented. Review proposals manually before acting on them.

The parser requires non-blank fields, correctly typed lists and numeric values,
valid source idea IDs, coverage of submitted ideas across concepts, and at least
two alternatives with distinct titles. Invalid structured output is retried **once**
with stricter instructions; persistent failure saves no partial results. API errors
return a safe diagnostic to the command loop. Concept and blueprint IDs are generated
by Java. Successful generations append to the same `Event` object, so prior results
remain available within the session; they are snapshots of inputs at generation time.
After changing ideas or constraints, generate new proposals and select the new IDs.
All events, ideas, constraints, concepts, and blueprints disappear on exit.

**Tests and optional real API check:** `mvn test` uses injected fake `AIModel`
implementations and makes no Gemini requests. The real-API smoke test is skipped
unless explicitly enabled. With local configuration present, run just that test:

```powershell
mvn test "-Dtest=GeminiSmokeTest" "-Dgemini.smoke=true"
```

This sends synthetic organizer ideas to the real API, performs F01 and F02, validates
the parsed objects and their storage, and may consume two to four requests (one retry
per operation). A missing credential causes an explicit failure when enabled.

| Requirement/pattern | Implementation |
| --- | --- |
| F01 Collaborative Idea Fusion | `EventIdea`, `EventConcept`, `EventPlanningAgent.synthesizeIdeas` |
| F02 Blueprint & Feasibility | `EventBlueprint`, `EventPlanningAgent.generateBlueprints`, deterministic numeric checks |
| Adapter target | `AIModel.generate` is the agent-facing abstraction |
| Adapter | `GeminiAdapter` implements `AIModel` and delegates to `GeminiAPIClient` |
| Adaptee | `GeminiAPIClient` wraps the official SDK and local configuration |
| Strategy context | `EventPlanningAgent.setPlanningStrategy` applies the selected guidance |
| Concrete strategies | `BudgetPriorityStrategy` (spending/reserve), `CommunityPriorityStrategy` (inclusion/relationships), `AttendancePriorityStrategy` (reach/capacity) |
| MVC and Facade | CLI → Controller → Facade → Agent/Services; `EventService` stores results in `Event` |

**Stage 1 scope adjustments:** Concepts and blueprints are immutable records with
typed lists rather than mutable UML placeholders; F07 revision is deferred. The
existing `Event.addIdea(String)` and `getIdeas()` remain compatible while attributed
`EventIdea` objects add source tracing. `AIModel` implements only the generation
operation used for planning and structured venue analysis; separate summarization
operations remain deferred. `constraints add/list` and `concepts list` are small
CLI additions for supplying Stage 1 inputs and retrieving proposal IDs. F03/F04
extend these workflows as documented in [the milestone guide](F03_F04_GUIDE.md).
F05/F06 add the budget and Places-tool workflows described in
[the Milestone 3B guide](F05_F06_GUIDE.md). F07–F10 workflows and new GUI screens remain deferred.

The Stage 1 design context follows.

**Stage 1 design completed; Stage 2 CLI and F01–F06 implemented.**

Stage 1 focuses on defining the system requirements, features, architecture, UML diagrams, design patterns, and AI-agent interactions before implementation begins.

## Course Information

**Course:** EECS 3311 – Software Design  
**Term:** Fall 2026  
**Project:** Course Project – AI Agent-Based Software


Milestone 4 (F07-F10) commands, architecture, approval boundaries and limitations: [MILESTONE4_GUIDE.md](MILESTONE4_GUIDE.md).


JavaFX dashboard (Milestone 5, Part A): [launch, feature workflows and verification](MILESTONE5_PART_A_GUIDE.md).
