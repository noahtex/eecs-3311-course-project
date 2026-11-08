# Milestone 3B: F05 budget/catering and F06 venue research

Run the existing CLI from its application working directory:

```powershell
cd ai-event-planner
mvn compile exec:java
```

Create or retrieve an event ID using the existing event commands. Replace all
`<eventId>` / `<itemId>` placeholders below with printed IDs. F01–F04 commands
continue to work. Storage is still in memory; exiting loses events and plans.

## Configuration and external functionality

The existing Gemini settings are unchanged. Live venue and catering-provider
discovery use **Google Places API (New), Text Search**. Add your own credential
locally as `GOOGLE_PLACES_API_KEY` in the application-directory `.env`, or set the
process environment variable. `.env.example` contains an empty placeholder only.
Enable Places API (New) and billing and restrict the key appropriately in your
Google Maps Platform project. Places requests can incur provider charges.

Configuration uses the existing UTF-8 `.env` parser, including quotes, optional
`export`, BOM handling, and process-environment precedence (including blank
environment values). `.env` resolves against the application process's working
directory, not the repository or Java source directory. `System.getenv()` does
not read the file. Places configuration does not require a Gemini key; Gemini is
needed for budget generation and venue interpretation.

The real client sends a timed HTTPS POST to
`https://places.googleapis.com/v1/places:searchText`, requests a fixed field mask,
and retrieves up to ten candidates from the first response. It maps provider IDs,
names, addresses, Google Maps links, business status, place types and supplied
accessibility options. See Google's [Text Search documentation](https://developers.google.com/maps/documentation/places/web-service/text-search)
and [field-mask documentation](https://developers.google.com/maps/documentation/places/web-service/choose-fields).
It uses Java 21's HTTP client and existing Jackson; no Maven dependency was added.
`module-info.java` adds `requires java.net.http`.

Provider-reported data is labelled **PROVIDER_REPORTED**, rather than independently
verified. In particular, a reported wheelchair entrance is not a guarantee of
full accessibility. Places does not supply this integration with event rental
quotes, room capacity, equipment inventory, event-date availability, catering
menus, per-person costs or allergen guarantees. These remain **UNKNOWN**.
No open-now/business-status field is treated as event-date booking availability.
No venue or caterer is automatically booked.

Missing configuration, HTTP failures and malformed provider responses produce
actionable errors with manual-entry guidance. Errors never include request
headers, credentials or raw response bodies. Live venue research failures leave
existing candidates intact. There are no demonstration venues in production.

Budget generation attempts live catering-provider discovery through the same
Places client. If that discovery is unavailable, generation continues with an
explicit warning and generic, unverified catering recommendations. Discovery
results establish only provider-reported business details, not quotes or dietary
suitability. Failed Gemini transport requests are not retried as malformed output.

## F05 commands and approval

| Command | Input / behavior |
| --- | --- |
| `budget generate <eventId>` | Prompts for currency, comma-separated dietary requirements, catering needs and priority (`budget`, `community`, `attendance`). Requests a structured categorized budget with catering recommendations. Shows the draft, item IDs, subtotal, remaining amount and warnings. Does not approve it. |
| `budget view <eventId>` | Shows draft if present, otherwise approved budget; prints item provenance, catering/provider data, assumptions and current validation warnings. |
| `budget edit <eventId> <itemId>` | Prompts for replacement category, description, positive quantity, price status, unit cost (unless unknown), and quote/source/assumption reference. Keeps the item ID and creates or updates a draft. |
| `budget validate <eventId>` | Recalculates and displays visible plan totals and warnings locally. No Gemini request. |
| `budget approve <eventId>` | Displays the current draft and warnings, revalidates and saves it to the existing Event. Rejects over-budget plans, unknown amounts and currency mismatches. Explicit approval can accept estimates without relabelling them confirmed. |
| `budget discard <eventId>` | Discards the draft and retains the last approved budget. |

Example:

```text
budget generate <eventId>
# Enter CAD, dietary needs, catering needs, and a planning priority.
budget view <eventId>
budget edit <eventId> <itemId>
# Enter replacement fields and a price source or explicit estimate assumption.
budget validate <eventId>
budget approve <eventId>
```

Currency defaults to CAD until explicitly configured; the original Event model
had no currency. Enter the denomination of the existing spending limit; this
does not convert or change its numeric amount. No exchange rate is inferred.
Budget generation includes event type, attendance, ideas, goals/free-text
constraints, existing blueprint details, dietary/catering needs, and selected
priority guidance in the AI context.

Money uses `BigDecimal`; JSON decimal costs are parsed without binary floating
point conversion. Unit prices round to two decimal places with HALF_UP; each
line total is quantity × rounded unit cost, rounded to two decimal places. Quantity
must be positive; unit cost must be non-negative. A zero cost is valid only as an
explicit known/estimated amount with its status and source, never as a fallback
for unknown data. `UNKNOWN` has a null amount and null total. Totals and balances
are labelled a **known subtotal** and **provisional remaining amount** if costs
are missing; unknown lines are not represented as free items. Unknown amounts
must be resolved to a documented estimate or confirmed amount before approval.

Every AI-generated price is constrained to `ESTIMATED` or `UNKNOWN`. A response
that claims `CONFIRMED` is rejected. Only an explicit planner edit can record a
confirmed price with a source/reference. Confirmation is a planner assertion;
this application does not independently authenticate quotes. Over-budget drafts
remain visible for revision and never change the Event's stated spending limit.
Warnings cover unverified catering details, tax/fee/delivery and dietary/allergen
confirmation. Approval does not make AI assumptions factual.

## F06 commands and manual fallback

| Command | Input / behavior |
| --- | --- |
| `venues research <eventId>` | Prompts for search terms (venue type/area/requirements); appends the event location and calls real Places discovery. Stores actual candidates on Event before any Gemini comparison. Reports empty results or provider errors. |
| `venues list <eventId>` | Lists stored retrieved and manual candidates, sources/timestamps, supported provider attributes, per-attribute evidence, and unknown availability. No API calls. |
| `venues compare <eventId>` | Prompts for wheelchair-entrance requirement and comma-separated equipment requirements. Uses Java checks and structured Gemini interpretation against all stored candidates. Prints rank, known-fit score, trade-offs, warnings and unknowns; stores the comparison on Event. Works for manual candidates without a Places key, but requires Gemini. |
| `venues add <eventId>` | Prompts for name, address, source/reference, evidence status, capacity, total event rental cost, wheelchair entrance and equipment; asks `yes` before storing. Does not require either API key. |

Example:

```text
venues research <eventId>
# Enter a venue search, e.g. accessible workshop spaces in the event area.
venues list <eventId>
venues add <eventId>
# If discovery is unavailable, enter real planner-supplied details.
# Leave unavailable attributes blank, then explicitly confirm storage.
venues compare <eventId>
# Enter wheelchair-entrance and equipment requirements.
```

Manual numeric/boolean/equipment fields left blank remain UNKNOWN. Enter `none`
for an explicitly empty equipment list. Entered attributes are either
`PLANNER_CONFIRMED` or `ESTIMATED`, with a source/reference. Manual entries cannot
masquerade as provider-reported data. The manual entry is saved only after `yes`;
EOF before confirmation saves nothing. Every manual candidate has a unique ID.

Java scores only represented provider-reported or planner-confirmed capacity,
cost, required wheelchair entrance, and required equipment; estimates earn no
confirmed-fit points. Known mismatches incur penalties. A simple address text
match to the event location preference earns a small location point bonus; this
is not geospatial/travel-time verification. Closed businesses are flagged. Ties
sort by candidate ID. Unknowns earn no confirmed-fit points and remain warnings.
Rental cost is compared with the entire event spending limit, with a warning
that other event expenses also require funding. Currency mismatches are not
converted. Scores measure partial evidence, not guaranteed feasibility.

Gemini receives all actual candidates, typed evidence, deterministic checks and
event context. It must return exactly one analysis/trade-off entry per real ID;
missing, invented or duplicate IDs are rejected. It cannot set rank, mutate
provider attributes or convert estimates into confirmed facts. Its prose is
always displayed as **UNVERIFIED AI interpretation**. Free-text accessibility,
location and other constraints require human review. Changing candidates,
structured venue needs or budget/catering settings invalidates stored comparisons.

## Created files and Stage 1 correspondence

Production paths below are relative to
`ai-event-planner/src/main/java/com/eventplanner/`.

| File | Purpose / Stage 1 mapping |
| --- | --- |
| `model/Budget.java` | F05 / UC05 / SD03 budget snapshot: ID, currency, priority, items, catering recommendations, retrieved provider snapshots and assumptions. |
| `model/BudgetItem.java` | F05 budget line with category, description, quantity, price status/source and calculated `total()`. |
| `model/CateringOption.java` | Stage 1 catering recommendation, optional real provider ID and dietary confirmation notes. |
| `model/Evidence.java` | Typed provenance for known, estimated and unknown venue attributes. |
| `model/VenueOption.java` | F06 / UC06 / SD03 actual/manual venue identity, address, source/time, provider attributes, capacity, rental cost, entrance accessibility and equipment evidence. |
| `model/ToolResult.java` | Stage 1 external result: source, typed candidates, warnings; retrieval timestamps live on candidates. |
| `model/VenueComparison.java` | Known-fit score and Java warnings alongside explicitly unverified AI analysis/trade-offs. |
| `service/BudgetService.java` | `totalExpenses`, `remainingBudget`, `shortfall`, `validateBudget`, `updateAllocation`, `saveBudget`. All numeric calculations and approval checks are deterministic. |
| `service/VenueService.java` | `evaluate` / `rank` compute suitability from represented evidence, independent of Gemini prose. |
| `tool/AITool.java` | Stage 1 `execute(String): ToolResult` abstraction, injectable for tests. |
| `tool/VenueSearchTool.java` | Stage 1 venue-tool adapter delegating `execute` to `PlacesAPIClient.searchPlaces`. |
| `tool/CateringSearchTool.java` | Stage 1 catering-provider discovery adapter using the same real Places client; does not claim menu/price verification. |
| `tool/PlacesAPIClient.java` | Stage 1 external integration; timed HTTPS transport, strict response mapping, sanitized failures and injectable fake transport. |
| `tool/PlacesConfiguration.java` | Independent Places-key loading using process environment and the existing local parser. |

Modified production classes:

- `Event`: `configureBudgetNeeds`, `configureVenueNeeds`, budget getters/storage,
  venue getters/storage and comparison getters/storage. The same existing Event
  owns all approved data; no duplicate event repository is introduced.
- `EventPlanningAgent`: `planBudgetAndCatering`, `compareVenues`, financial JSON
  decimal parsing, commercial/provider context mapping. Reuses the existing
  structured-output parser/retry path, with at most one malformed-output retry.
- `EventPlanningFacade`: new methods for configuring needs, generating/viewing/
  editing/validating/approving/discarding budgets, deterministic totals/balances,
  and researching/listing/adding/comparing venues. Adds injectable venue/catering
  tools while preserving existing constructors. Budget drafts are separate from
  approved snapshots and are removed when an Event is deleted.
- `EventPlanningController`: corresponding delegating application operations.
- `EventPlanningCLI`: command dispatch, prompts, provenance/totals display,
  explicit approval/manual-entry confirmation, and updated help.
- `GeminiConfiguration`: exposes `readLocalValues(Path)` to reuse its parser;
  Gemini key/model precedence and lazy loading remain unchanged.
- `module-info.java`: adds the JDK HTTP module. `pom.xml` needs no change.
- `.env.example`, `README.md`, and this guide document configuration and usage.

Design refinements: immutable typed records replace mutable UML placeholders;
null plus explicit UNKNOWN replaces ambiguous absent amounts; `BigDecimal`
replaces floating point for new budget calculations; per-attribute evidence
replaces unstructured tool content. `PlacesAPIClient.searchPlaces` returns typed
`ToolResult` rather than raw text. Stage 1 `AIModel.analyze()` is realized through
the existing `AIModel.generate()` structured-comparison operation, avoiding a
second AI abstraction. The controller/facade flow remains unchanged. No F08
Observer behavior, GUI screens, exchange service or booking system is introduced.

## Verification

```powershell
cd ai-event-planner
mvn test
```

New tests use fake AIModel implementations, injected fake AITools, fake HTTP
transport responses and temporary local configuration files. They never call
Gemini or Places. New test files under `src/test/java/com/eventplanner/`:

- `support/CommercialFixtures.java`: synthetic test-only budgets/providers/JSON.
- `service/BudgetServiceTest.java`: exact totals, remaining funds/shortfall,
  over-budget/unknown warnings, negative/invalid amounts, immutable edits and approval.
- `service/VenueServiceTest.java`: known/unknown/estimated ranking and known
  capacity, cost, equipment and accessibility mismatches.
- `tool/PlacesAPIClientTest.java`: configuration precedence/missing configuration,
  transport request/mapping, empty results, HTTP/network/malformed-data failures
  and interrupt handling, all without live requests.
- `agent/CommercialAgentTest.java`: provider/context use, exact decimal parsing,
  schema/source-ID validation, AI-estimate restrictions, retry limits and comparisons.
- `facade/CommercialWorkflowTest.java`: controller workflow, Event storage,
  approval/editing, manual candidates, missing providers and data preservation.
- `view/CommercialCLITest.java`: all requested commands, manual fallback after
  external failure, invalid input/approval recovery and EOF cancellation.

Verification on November 7, 2026: `mvn compile` succeeded and the final `mvn test`
run reported **96 tests, 0 failures, 0 errors, 1 skipped** (95 passed). This includes
the existing F01–F04 regression suite and 30 new tests for this milestone.

Live Places/Gemini integration was not exercised during implementation. No
working credential, enabled billing/project or live result is claimed. The real
transport is implemented; actual discovery requires your configured provider
account. The pre-existing live Gemini smoke test remains opt-in and skipped by
regular Maven tests. All F01–F04 regression tests remain part of the suite.
