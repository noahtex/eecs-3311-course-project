# F02: approved execution blueprint

Planit retains every generated alternative for comparison. Each event now has two separate references:

- `selectedBlueprintId`: the organizer's pending choice.
- `approvedBlueprintId`: the authoritative alternative for subsequent task and schedule generation. A null value means no blueprint has been approved.

Selecting a different blueprint does not revoke or replace the currently approved one. Only explicit approval changes the execution blueprint. Both references must identify blueprints owned by the event. Approval is an organizer planning decision; generated estimates and external assumptions remain unverified.

## GUI

Open **Blueprints**, inspect an alternative, click **Select blueprint**, then **Approve selected blueprint** and confirm. The execution card shows the approved and pending names; the table identifies approved, pending and comparison-only alternatives. Cancelling the confirmation keeps the previous approved blueprint.

When approval changes, existing approved tasks, schedules and drafts remain intact. Persistent review warnings appear in the Tasks and Schedule panels and their CLI views. Review and revise or regenerate those plans through their existing draft/approval workflows. Blueprint approval never automatically approves generated tasks or schedules.

## CLI

```text
blueprint list <eventId>
blueprint view <eventId> <blueprintId>
blueprint select <eventId> <blueprintId>
blueprint approve <eventId>
```

`blueprint approve` displays the selected blueprint and requires `yes` before approval. The existing generate/list/view commands remain available. List/view identify execution status, and list also shows the approved and pending selections.

## Execution and compatibility rules

- With an approved blueprint, task and schedule requests receive only that blueprint's ID, title, summary, agenda, checklist and resources. Other alternatives, including a different pending selection, are excluded from that execution context.
- If alternatives exist but none is approved, task/schedule generation fails before an AI request with a selection-and-approval instruction.
- If no blueprints exist, the pre-existing event-details/ideas/constraints workflow remains available. The execution context explicitly identifies this limited fallback and contains no blueprint instructions. This preserves direct F03/F04 use for existing events without silently choosing an alternative.
- Existing approved tasks/schedules remain accessible after loading an old event even if it has unapproved blueprint alternatives. Approval is required for new generation, not for merely viewing or editing saved work.

The version-1 JSON event DTO adds nullable `selectedBlueprintId` and `approvedBlueprintId`. Older JSON missing these fields loads with both null. Loading does not rewrite the file; the next successful mutation writes the new fields. Existing IDs, records, approval states, decimal precision/scale and strict validation of original fields are preserved. Invalid blueprint references still reject the file without changing it. Snapshots and save-failure rollback preserve both references.

## Scope and limitations

This correction affects F02 selection and F03/F04 execution only. Budget, venue and adaptive-analysis prompts are unchanged. It does not automatically rewrite or prove semantic alignment of existing execution plans, change blueprint content, verify estimates, add venue bookings, or add an unapprove/delete operation. Existing review warnings remain advisory and use the application's current warning lifecycle.

## Code and tests

Production classes changed: `Event`, `EventPlanningAgent`, `EventPlanningFacade`, `EventPlanningController`, `BlueprintsPanel`, `TasksPanel`, `SchedulePanel`, `EventPlanningCLI`, `PlanData`, `PlanDataMapper`, `JsonPlanRepository`. `EventBlueprint` remains an immutable alternative and required no change. API transport, styling, dependencies and resource lifecycle are unchanged.

Added tests:

- `BlueprintSelectionTest`: event ownership; explicit approval; no-approval rejection before AI; only the approved alternative in both execution prompts; pending selection isolation; event-only fallback; retained approved/draft plans and review flags; snapshot references.
- `BlueprintSelectionPersistenceTest`: pending and approved references across restarts; preserved plans/warnings; legacy JSON loading and subsequent upgrade; monetary-value preservation; invalid references/missing original properties; save-failure rollback.
- `BlueprintSelectionCLITest`: commands, statuses, validation, approval and cancellation.
- `JavaFxSmokeTest.blueprintSelectionAndApprovalButtonsKeepAlternativesAndFlagRetainedPlans`: real buttons, confirmation/cancellation, names, retained alternatives/plans and visible review warnings, using offline backend fixtures.

Updated `PersistenceTest.generatedConceptBlueprintTaskScheduleAndBudgetDraftsAreAutomaticallySaved` to select and approve its generated blueprint before execution and assert that approval survives restart. Its existing plan/draft assertions remain.

## Executed verification (November 13, 2026)

| Command | Result | Tests reported | Failures | Errors | Skipped |
|---|---|---:|---:|---:|---:|
| `mvn test -B -ntp` (final ordinary run) | BUILD SUCCESS | 173 | 0 | 0 | 15 |
| `mvn clean test -Dgui.smoke=true -B -ntp` | Clean failed: Windows could not delete `target/classes`; test phase not reached | — | — | — | — |
| `mvn test -Dgui.smoke=true -B -ntp` | BUILD SUCCESS | 173 | 0 | 0 | 1 |

The last run recompiled all 86 production and 36 test source files. It executed 172 tests successfully, including all 14 JavaFX smoke tests: actual `App.init/start/stop`, CSS/icons/synthetic-store startup, existing interactions and the new blueprint controls. Only the opt-in live Gemini smoke test was skipped. In the ordinary run, the 14 desktop tests were also skipped.

An earlier intermediate test run caught a decimal-scale regression introduced by tree-based legacy loading (172 reported, 1 failure, 0 errors, 14 skipped). It was corrected by reading tree numbers as exact BigDecimals without stripping trailing zeros; the existing full-graph equality assertion and new legacy monetary-data check now pass without weakening assertions.

No paid external API calls were made, and no real user store or credentials were opened. Tests used synthetic data and injected providers. Maven packaging was not rerun for this focused change. `git diff --check` passed. No files were staged, committed or pushed. Existing audit findings outside this correction remain unchanged; the original final audit document is a historical snapshot from before this fix.
