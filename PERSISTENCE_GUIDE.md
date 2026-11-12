# Planit local JSON persistence

Both production entry points (`App` and `CLIApp`) open the same JSON-backed facade. Saved events load before event lists are displayed. Existing constructors that inject an `EventService` retain an explicit in-memory repository for isolated tests/embedded sessions; a repository can also be injected directly.

## Location

| Platform | Default save file |
| --- | --- |
| Windows | `%LOCALAPPDATA%\Planit\planit.json` (fallback: `user.home\AppData\Local\Planit\planit.json`) |
| macOS | `~/Library/Application Support/Planit/planit.json` |
| Linux | `$XDG_DATA_HOME/planit/planit.json`, or `~/.local/share/planit/planit.json` |

On the verified Windows account the path is **`C:\Users\Noah\AppData\Local\Planit\planit.json`**. A neighboring `planit.lock` coordinates exclusive access. The directory and lock are created at startup; the JSON file is first written after a successful meaningful mutation, not merely when the window opens.

Overrides, in order: the **actual application JVM's** `planit.dataDir` system property, then the `PLANIT_DATA_DIR` environment variable, then the platform default. The environment override is convenient for both Maven launchers because it is inherited by their child JVMs. For example, set `$env:PLANIT_DATA_DIR = 'D:\PlanitData'` in PowerShell before launching either interface. Supplying an arbitrary Maven `-D` property is not equivalent to supplying a JVM option to every forked launcher.

Tests inject `JsonPlanRepository(@TempDir)` and, for the real App smoke test, set `planit.dataDir` to a temporary directory before `App.init()`. Unit tests never use the user's production data directory.

## Data and identity

The version-1 document has an event-data list and an event-ID-keyed draft map. Explicit DTOs contain only domain data. Immutable model records are used directly for tasks, sessions, budgets, evidence, communications and reports. Events, users and teams have controlled DTO mappings rather than automatic live-graph serialization.

Persisted data includes:

- Event identity/metadata, local start/end, target attendance/spending limit, currency, constraints, catering/accessibility/equipment requirements.
- Team identity/name, members and their planning profiles, canonical team-event links, and shared member-object relationships. Separate event profiles remain separate even when they use the same person ID.
- Organizer ideas (including submission times and original IDs), concepts, blueprint alternatives and their source references.
- Approved tasks/schedules/budgets **and separate task, schedule and budget drafts**, including task proposal warnings. A missing draft is distinct from an empty task draft.
- Speaker availability, resources, venue candidates, suitability comparisons, provider attributes and per-attribute evidence/provenance/status.
- Pending real-change proposals, current confirmed event facts, Observer review warnings and contingency history.
- Communications, exact draft text, source facts and approval flags.
- Recorded post-event outcomes and stored reports, including their own outcome snapshots and nullable actual values. UNKNOWN stays distinct from recorded zero; decimal amounts and timestamp precision survive round trips.

IDs are never regenerated during restoration. Canonical team/event references and shared user references are rebuilt from data, then fresh task/schedule/budget Observer services are attached. Loading does not replay AI requests, approvals, or event-change notifications and does not silently reapprove or invalidate existing plans.

Java `LocalDateTime` and `Instant` use strict ISO Jackson serializers/deserializers with the existing Jackson dependency. The current domain has event-local date/times and no timezone-ID field; these values are preserved without host-timezone conversion. No timezone, venue booking/selection, or historical change-audit field is invented where the backend does not have one. Confirmed updates are represented by their persisted current event facts and review warnings.

Runtime services, observers, locks, networking clients, credentials, GUI controls, transient simulations, FAQ displays and unsent form buffers are not serialized. Simulations remain hypothetical and do not change the saved original. API configuration/environment objects are never traversed by serialization.

## Save boundary and responsiveness

All meaningful controller operations delegate to facade mutation transactions. They serialize competing mutations, run existing planning/validation, compare explicit before/after snapshots and save if state actually changed. This covers creation/deletion, nested metadata/member/resource edits, generation, revisions, draft discards, approvals, pending-change proposals/confirmation, communication edits, and post-event recording/analysis. GUI event creation and its end time are saved in one transaction.

Read-only access, navigation, selection, validation views, FAQ answers and hypothetical simulations do not write the store. Closing does not perform a catch-all save of arbitrary live objects; successful application commands have already saved. Directly modifying objects returned by an embedded facade bypasses its transaction boundary and is not an autosave API—application code should use controller/facade commands.

Every GUI mutation, including formerly small form edits, now runs through the existing serialized Task executor because JSON I/O can block. Controls are read/validated on FX before submission. Success messages are shown only after the save returns. Startup loads in `Application.init()` off FX. Existing loading states, navigation, event-scoped completion and queued cleanup remain.

## File safety and conflicting writers

- A session holds an OS file lock from repository open until owned facade cleanup. A second GUI or CLI process is refused with a clear message. The persistent lock file is intentionally not deleted; its existence alone does not indicate a live lock, and the OS releases locks after process exit.
- A SHA-256 content check additionally detects external modification/deletion before replacement and refuses to overwrite it. Restart to load external changes. This is not a merge system and cannot control programs that ignore file locks.
- Writes use a unique temporary file beside the save file, flush it with `FileChannel.force(true)`, and replace with `ATOMIC_MOVE` when supported. The fallback uses normal same-directory replacement. Temporary files are cleaned up where possible.
- Failed serialization/write operations raise a safe storage error and restore the facade's pre-operation data/drafts. The GUI refreshes the prior state and the CLI reports an error instead of printing success. Previously borrowed domain object references may become stale after rollback; look up by ID again, as the GUI does.
- Malformed, unreadable, semantically invalid, or unsupported-version documents stop startup. They are not replaced with an empty save. GUI startup reports an error; CLI startup prints the storage error. No automatic reset or silent migration is attempted.

For a corrupt file, close Planit, preserve a copy of the existing JSON, and restore a known-good copy or repair it before restarting. To deliberately start separately with an empty store, choose a new data directory. The application does not maintain an automatic backup history; copying `planit.json` while both interfaces are closed is the simplest manual backup.

Local-filesystem locking/rename behavior is the supported model. Full power-loss durability, unsupported atomic moves, and noncooperating external writers cannot be guaranteed; the implementation is not a multi-user/network storage system. The full document is rewritten per mutation, so very large stores can take longer, while the GUI remains responsive.

## Verification

Automated tests use temporary directories and injected AI/tool responses, without external API calls. `PersistenceTest` covers complete graph round trips, stable IDs/identity links, drafts versus approved plans, generated F01–F10 data, nullable actuals versus zero, pending/stale changes, confirmed updates and restored Observer behavior, deletion, malformed/semantic corruption, failed replacement rollback, external changes, exclusive locking, concurrent mutations and read-only operations.

`CLIAppLifecycleTest` verifies a CLI mutation is saved through the shared facade and the lock is released. Desktop checks include loading a real App against a temporary store, calendar-based creation, blocked-save responsiveness, navigation/event switching, and clear failure/rollback behavior. Existing F01–F10/CLI tests remain in the suite.

Commands:

```powershell
cd ai-event-planner
mvn test
mvn '-Dgui.smoke=true' test
mvn javafx:run
```

Live Gemini/Places provider requests are not part of persistence tests. Actual machine power-loss and every filesystem fallback are not tested.

Verified results during implementation:

- `mvn test`: BUILD SUCCESS; 159 tests, zero failures/errors, 11 skipped (ten opt-in desktop tests plus live Gemini).
- Desktop-enabled full suite: BUILD SUCCESS; 159 tests, zero failures/errors, one skipped (live Gemini). Final focused desktop run: all ten tests passed, including an initial event list populated from a previously saved JSON document.
- `mvn javafx:run`: the real persistent Planit window launched and responded, then closed normally with Maven BUILD SUCCESS.
- A separate real CLI process was refused while that GUI held the store. After GUI cleanup, the CLI opened the same default directory and exited normally. No sample events were added to the user's default store during this launcher check.
- `git diff --check` passed. No staging, commits or pushes were performed.
