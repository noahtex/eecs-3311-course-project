# Planit Stage 2 UML

Verified against the implementation on **November 16, 2026**. Start with the architecture diagram, then use the focused domain/pattern diagrams for details. These diagrams supersede the Stage 1 UML for describing the implemented system. The original `../*.uxf` and `../*.png` remain historical design artifacts so existing reports stay unchanged.

Every diagram has **editable PlantUML source, scalable SVG and PNG**. SVG is preferred when inserting a diagram into a report or viewing long method signatures. Diagrams show selected responsibilities, not an exhaustive listing of getters, constructors, validation helpers or Java library types.

| Diagram | Editable source | SVG | PNG |
| --- | --- | --- | --- |
| MVC and Facade architecture | [Source](class-01-architecture.puml) | [SVG](class-01-architecture.svg) | [PNG](class-01-architecture.png) |
| Planning domain, blueprint references and execution | [Source](class-02-planning-domain.puml) | [SVG](class-02-planning-domain.svg) | [PNG](class-02-planning-domain.png) |
| Commercial evidence and adaptive outcomes | [Source](class-03-commercial-outcomes.puml) | [SVG](class-03-commercial-outcomes.svg) | [PNG](class-03-commercial-outcomes.png) |
| Strategy, Adapter and external tools | [Source](class-04-strategy-adapter-tools.puml) | [SVG](class-04-strategy-adapter-tools.svg) | [PNG](class-04-strategy-adapter-tools.png) |
| Observer and adaptive service | [Source](class-05-observer-adaptive.puml) | [SVG](class-05-observer-adaptive.svg) | [PNG](class-05-observer-adaptive.png) |
| Repository and persisted snapshots | [Source](class-06-persistence.puml) | [SVG](class-06-persistence.svg) | [PNG](class-06-persistence.png) |
| Implemented F01–F10 use cases | [Source](use-case-f01-f10.puml) | [SVG](use-case-f01-f10.svg) | [PNG](use-case-f01-f10.png) |
| SD01a: Idea fusion and blueprint generation | [Source](sequence-SD01a-ideas-blueprints.puml) | [SVG](sequence-SD01a-ideas-blueprints.svg) | [PNG](sequence-SD01a-ideas-blueprints.png) |
| SD01b: Blueprint selection and approval | [Source](sequence-SD01b-blueprint-approval.puml) | [SVG](sequence-SD01b-blueprint-approval.svg) | [PNG](sequence-SD01b-blueprint-approval.png) |
| SD02: Task/schedule drafts and approvals | [Source](sequence-SD02-tasks-schedule.puml) | [SVG](sequence-SD02-tasks-schedule.svg) | [PNG](sequence-SD02-tasks-schedule.png) |
| SD03a: Budget/catering and approval | [Source](sequence-SD03a-budget.puml) | [SVG](sequence-SD03a-budget.svg) | [PNG](sequence-SD03a-budget.png) |
| SD03b: Venue research and comparison | [Source](sequence-SD03b-venues.puml) | [SVG](sequence-SD03b-venues.svg) | [PNG](sequence-SD03b-venues.png) |
| SD04a: Isolated simulation and GUI preview | [Source](sequence-SD04a-simulation.puml) | [SVG](sequence-SD04a-simulation.svg) | [PNG](sequence-SD04a-simulation.png) |
| SD04b: Real confirmation, Observer, contingency | [Source](sequence-SD04b-update-contingency.puml) | [SVG](sequence-SD04b-update-contingency.svg) | [PNG](sequence-SD04b-update-contingency.png) |
| SD05: Communication drafts and local FAQ | [Source](sequence-SD05-communications-faq.puml) | [SVG](sequence-SD05-communications-faq.svg) | [PNG](sequence-SD05-communications-faq.png) |
| SD06: Recorded outcomes and analysis | [Source](sequence-SD06-post-event.puml) | [SVG](sequence-SD06-post-event.svg) | [PNG](sequence-SD06-post-event.png) |
| SD07: Startup, persistence and save rollback | [Source](sequence-SD07-persistence.puml) | [SVG](sequence-SD07-persistence.svg) | [PNG](sequence-SD07-persistence.png) |
| SD08: Shared AI generation and bounded retry | [Source](sequence-SD08-ai-generation.puml) | [SVG](sequence-SD08-ai-generation.svg) | [PNG](sequence-SD08-ai-generation.png) |

## Notation and scope

- Solid hollow triangle: class inheritance. Dashed hollow triangle: interface realization. Plain solid arrows: navigable associations. Dashed arrows: dependencies. Filled diamond: composition only where an object is exclusively created/retained (facade → adaptive service). Hollow diamond: shared aggregation (team members).
- Immutable domain records can be shared across snapshots; their collection relationships deliberately do not assert exclusive composite ownership. ID-based links (`assignedUserId`, `speakerId`, `conceptId`, `venueId`) are labelled dependencies rather than direct object references.
- Record compartments list component schemas; generated accessors are implicit. `Channel` means `Communication.Channel`, task `Status` means `Task.Status`, and each record's unqualified status/enum name refers to its own nested enum. `Event` end time, approved schedule/budget and actuals may be absent.
- Sequence boundaries labelled “panel / CLI” mean alternative actual views, not a newly invented shared Java class. Combined observer lifelines mean one callback per actual service. Dashed return arrows labelled “through controller” abbreviate forwarding returns; private helpers/getters and JavaFX scheduling are sometimes collapsed. SDK request labels are prose, not invented SDK method signatures.
- `ref` frames point to the companion SD07 persistence and SD08 AI interactions; they do not introduce application methods. A save occurs only if the facade's captured domain/draft state changes. Errors propagate as failures, not successful results.
- The use-case actor represents actual local interaction; a note summarizes external-service participation across features, with explicit API relationships in class-04 and SD03/SD08. There is no authentication, dedicated attendee portal, weather integration, venue booking or message publication. Workflow order is not modelled using misleading UML `include`/`extend` relationships.

## Reproduce exports and checks

Exports were produced locally with **PlantUML 1.2025.10** and Java headless rendering: Smetana for class diagrams, default Graphviz layout for use cases and PlantUML's sequence renderer. No diagram/source was uploaded to an external rendering service. The renderer was downloaded from [Maven Central](https://repo.maven.apache.org/maven2/net/sourceforge/plantuml/plantuml/1.2025.10/plantuml-1.2025.10.jar) into ignored `tools/plantuml.jar`; it is not an application dependency or a submission artifact.

Downloaded JAR SHA-256: `4a01ea09b317180fb8e7eef712dfdca725409d2ee1919e4b5adfe9d8362b6fe5`.

From the repository root, when local PowerShell policy permits scripts:

```powershell
.\diagrams\stage2\render.ps1
python diagrams/stage2/verify_uml.py
```

Alternatively provide a renderer with `render.ps1 -PlantUmlJar 'path/to/plantuml.jar'`. This machine's script execution policy blocked direct script invocation during preparation; rendering succeeded using the equivalent direct Java commands, without changing execution policy. To render one file directly:

```powershell
java '-Djava.awt.headless=true' -jar diagrams/stage2/tools/plantuml.jar -charset UTF-8 -failfast2 -tsvg diagrams/stage2/class-01-architecture.puml
java '-Djava.awt.headless=true' -jar diagrams/stage2/tools/plantuml.jar -charset UTF-8 -failfast2 -tpng diagrams/stage2/class-01-architecture.puml
```

Run the verification script after exports finish. It checks Java type/member-name traceability, SVG XML and PNG signatures/dimensions. It does not prove full signatures, associations, call order or application behavior; those were reviewed against source. The detailed change log and remaining implementation discrepancies are in [UML_VERIFICATION.md](../../UML_VERIFICATION.md).
