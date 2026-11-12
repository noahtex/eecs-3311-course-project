# Milestone 5, Part B: F07–F10 desktop workflows

Run from `ai-event-planner`:

```powershell
mvn javafx:run
```

Use the existing Gemini configuration for AI generation. No new dependencies or credentials are required. The FAQ uses the existing conservative backend answer rules rather than making an AI request. Events, inputs, generated histories and transient results last only for the current application session.

## Controls and application operations

| Page / control | Existing controller operations |
| --- | --- |
| Scenario Simulator: Run simulation | `simulateScenario(eventId, request)` |
| Clear simulation | Clears only that event's GUI input and transient result |
| Plan Updates: Preview changes & impacts | `simulateScenario`, then `proposeChangeFields` with the exact analyzed fields |
| Confirm Changes / Discard proposed changes | `pendingChanges`, `confirmChange(eventId, true/false)` |
| Current event details / Observer review warnings | `viewEvent`, then the event's existing getters |
| Generate / inspect contingencies | `generateContingency`, `listContingencies` |
| Communications: Generate communication draft | `generateCommunication` with EMAIL, INSTAGRAM or WHATSAPP and a nonblank tone |
| Stored communications / Save draft edits / Approve draft | `listCommunications`, `editCommunication`, `approveCommunication` |
| Ask attendee FAQ | `askFaq` |
| Post-Event Review: Save actual outcomes | `recordPostEvent` |
| Generate post-event analysis / stored report selector | `analyzePostEvent`, `viewPostEventReports` |

The backend accepts arbitrary nonblank tones. The GUI offers friendly, formal and casual; it does not introduce a new backend tone restriction. Planner-edited communication text remains the planner's responsibility, as in the CLI. Approval neither sends nor posts content.

## Safety and responsiveness

- All generation, analysis, save, approval and confirmation work in these pages uses the existing serialized JavaFX Task executor. Controls and per-event GUI caches update only on the FX thread.
- Progress appears in the footer. Mutation controls are unavailable during requests, while navigation and event selection remain available. The selected view reloads after completion; domain data is not rendered concurrently with worker mutations.
- Inputs, simulations, FAQ answers and draft editor buffers are scoped to event IDs. A result from an earlier event is retained for that event without redirecting the current selection.
- Event selector labels are cached on FX rather than reading changing event fields during worker execution.
- Simulations remain hypothetical. A real update first analyzes an isolated snapshot, then submits those exact fields as a pending proposal. Confirm Changes requires an additional dialog and uses the backend's stale-proposal guard. Approved task, schedule and budget plans are retained; Observer warnings appear on the update page.
- Communication edits must be saved before approval. Saving returns a communication to DRAFT. The backend rejects approval if its source facts have changed. FAQ results are hidden after event facts change.
- Blank actual attendance/expenses remain UNKNOWN, while zero remains a recorded value. Feedback uses one entry per line; task outcomes use approved task IDs separated by commas. Negative/invalid numbers and duplicate/overlapping task outcomes are rejected. Backend validation rejects unknown task IDs.
- Analysis uses saved outcomes only. Unsaved edits must be saved first. Stored reports show their own recorded outcomes rather than the current unsaved form.
- Existing cleanup ownership remains intact: closing the GUI queues owned resource cleanup behind active work and shuts down the executor without blocking FX or closing resources during a request.

## Classes

New GUI classes: `ScenarioPanel`, `PlanUpdatesPanel`, `CommunicationsPanel`, `PostEventPanel`, `PartBState`, and `PartBWorkflow`. `Dashboard` adds navigation, event-scoped state and a typed FX completion callback; `GuiInputs` adds post-event form parsing. No controller, facade, CLI, model, adapter or external dependency was changed.

Tests: `PartBWorkflowTest` covers exact analyzed-field delegation, failed previews, numeric validation, UNKNOWN versus zero, and conflicting task outcomes. `JavaFxSmokeTest` also exercises F07–F10 buttons through the real controller/facade with an injected test AI model: event switching, simulation clearing, preview/confirmation, contingency storage, communication generation/edit/approval, conservative FAQ answers, actual-data saving and report generation. Its existing navigation test renders all F01–F10 pages.

## Verification and manual checks

```powershell
mvn test
mvn '-Dgui.smoke=true' test
```

Desktop tests open temporary windows and use test-only AI responses; the production application always calls the existing backend integrations. Live Gemini and Places behavior still requires manual verification with the configured account and network.

Verified during implementation:

- `mvn test`: BUILD SUCCESS, 136 tests, zero failures/errors, six skipped (five desktop tests plus the opt-in live Gemini test).
- `mvn '-Dgui.smoke=true' test`: BUILD SUCCESS, 136 tests, zero failures/errors, one skipped (live Gemini).
- `mvn javafx:run`: the real ClubPlan window launched, Windows reported it responding, and it was closed normally. Maven exited with BUILD SUCCESS.
- `git diff --check`: passed. No staging, commits or pushes were performed.

1. Create two events. On the first, simulate increased attendance; move/resize the window and select the second event during generation. Confirm the second never displays the first event's simulation. Switch back and inspect the hypothetical result; the real event must be unchanged.
2. Preview a lower budget on Plan Updates. Inspect interpreted changes and impacts, cancel the confirmation dialog, and confirm the event is unchanged. Then confirm explicitly and inspect the updated details and Observer warnings. Approved plans must remain available for review.
3. Generate a contingency and inspect its risk, impact, response, resources and assumptions. Navigate away and back; it must remain in the stored list.
4. Generate drafts for each communication channel/tone. Edit and save a draft, then approve it. Confirm that saving edits returns it to DRAFT. Change event facts and confirm stale communication approval is rejected. Ask FAQs about the date and an unconfirmed ticket price; missing information must remain unconfirmed.
5. Save post-event outcomes with blanks and then with recorded zeros. Test invalid/negative numbers and unknown or overlapping task IDs. Generate reports after saving and inspect attendance/expense differences, task statistics, feedback attribution, missing information and lessons. Select older reports to inspect their saved outcomes.
6. Test network/authentication failures and retry. Progress must stop, a safe error must appear, and operations must become available again. Close the app during a request and verify it closes without interrupting shared resources in use.

Automated tests cover the offline workflows, rendering, event isolation, error recovery and cleanup ordering. They do not establish live API quality, network failure behavior against the providers, or visual usability on every screen size. Existing backend FAQ coverage is intentionally conservative, and historical report currency follows the event's planning currency because reports do not have a separate currency field.
