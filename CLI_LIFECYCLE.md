# CLI resource lifecycle and the Okio Watchdog

Run the CLI in its own JVM from `ai-event-planner`:

```powershell
mvn compile exec:exec@cli
```

The `cli` execution uses Maven's Java executable and dependency classpath. It keeps
the same CLI entry point, working directory, environment and `.env` loading rules.
The GUI continues to use `mvn javafx:run`. The legacy `mvn compile exec:java` command
is still configured, with Maven's thread cleanup diagnostics enabled.

## Cause

The dependency chain is `google-genai:1.80.0` → `okhttp:4.12.0` →
`okio-jvm:3.6.0`. Okio's `AsyncTimeout` starts a shared daemon named
`Okio Watchdog`. This daemon ignores interruption and exits after its timeout queue
has stayed idle for 60 seconds. It has no supported public shutdown method.
`exec:java` runs inside Maven's JVM and tries to interrupt/join application daemon
threads; its 15-second default wait is shorter than Okio's idle retirement interval.
This can produce the warning even when all request resources were closed correctly.

The original Gemini wrapper already used SDK `Client.close()` per request, so the
warning alone was not evidence of an unclosed request or leaked connection pool.
Places uses the JDK `java.net.http.HttpClient`, not OkHttp/Okio, and already closes
that client in a per-request try-with-resources block after consuming the response.

## Ownership and cleanup

* `CLIApp` owns its facade and closes it with try-with-resources on `exit`, EOF or
  an unexpected exception. Its main method also closes the scanner it creates.
* `EventPlanningCLI` borrows input/output and its controller; it does not close
  caller-owned streams or own networking clients. Embedded callers close their facade.
* Facade → agent → `AIModel.close()` → `GeminiAdapter` → `GeminiAPIClient.close()`
  propagates cleanup. Injecting an agent/model transfers lifecycle ownership to
  its enclosing facade/agent. Fakes without resources retain a default no-op close.
* `GeminiAPIClient` opens one SDK client lazily and reuses it throughout the session.
  Closing it is idempotent, does not allocate a client if unused, and prevents further
  requests. Request and close synchronize so cleanup cannot race an active request.
* SDK `Client.close()` delegates to its HTTP client's supported cleanup: dispatcher
  executor shutdown, connection-pool eviction, and cache close when present. The
  application does not access SDK internals or duplicate those calls.
* Places' existing request-level cleanup remains unchanged. Injected fake Places
  transports keep their existing caller-owned contract.

`exec:exec@cli` uses ordinary JVM lifecycle semantics after application cleanup:
once only daemon threads remain, the child JVM can exit. This avoids Maven's
in-process daemon-interruption mismatch without disabling warnings, forcibly
stopping threads, waiting an arbitrary interval, or calling `System.exit()`.

## Verification and limits

`mvn test` includes fake-resource tests for lazy initialization, session reuse,
idempotent close, rejection after close, sanitized cleanup errors, cleanup after
request failure, and propagation through the facade on CLI exit/EOF/unexpected
failure. Unit tests do not call real Gemini or Places endpoints.

The shared Okio watchdog may still trigger the warning when explicitly using
`exec:java` soon after a Gemini request. Application close does not control that
global daemon. There is no application-side supported shutdown API for it in the
pinned dependency. Cleanup also cannot be guaranteed after OS process termination
or a JVM crash. Gemini configuration is captured at the session's first AI request;
restart the CLI to pick up credential/model changes after that point.

Sources:

* [SDK Client.close](https://github.com/googleapis/java-genai/blob/v1.80.0/src/main/java/com/google/genai/Client.java)
* [SDK dispatcher/pool/cache cleanup](https://github.com/googleapis/java-genai/blob/v1.80.0/src/main/java/com/google/genai/ApiClient.java)
* [Okio 3.6 watchdog behavior](https://github.com/square/okio/blob/parent-3.6.0/okio/src/jvmMain/kotlin/okio/AsyncTimeout.kt)
* [Maven in-process thread management](https://www.mojohaus.org/exec-maven-plugin/usage.html)
* [Maven separate-process Java launcher](https://www.mojohaus.org/exec-maven-plugin/examples/example-exec-for-java-programs.html)
