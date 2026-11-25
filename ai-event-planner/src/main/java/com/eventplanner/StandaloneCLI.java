package com.eventplanner;

import com.eventplanner.ai.GeminiConfiguration;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** Standalone argument and task-file boundary; planning remains in the controller/facade. */
final class StandaloneCLI {
    private StandaloneCLI() { }
    static final String HELP = """
            Usage: java -jar ai-event-planner-1.0-SNAPSHOT-cli.jar [--help | --llm gemini --input <task-file>]
            No arguments starts the interactive CLI. Task files are UTF-8 key=value lines:
              action=fuse-ideas
              eventId=<saved-event-id>
            The saved event must already have organizer ideas. Gemini credentials are read
            from GEMINI_API_KEY or .env in the current working directory; GEMINI_MODEL
            selects the model. Example: java -jar "C:\\Planit\\planit-cli.jar" --llm gemini --input "C:\\My Tasks\\task.txt"
            Exit codes: 0 success, 2 invalid arguments/task, 3 unreadable input file,
            4 missing Gemini credentials, 5 operation or storage failure.
            """;
    record Options(boolean help, Path input) { }
    record Task(String eventId) { }

    static Options parse(String[] args) {
        if (args.length == 0) return new Options(false, null);
        if (args.length == 1 && args[0].equals("--help")) return new Options(true, null);
        String llm = null, input = null;
        for (int i = 0; i < args.length; i++) {
            String flag = args[i];
            if ((!flag.equals("--llm") && !flag.equals("--input")) || i + 1 == args.length)
                throw new IllegalArgumentException("Expected --llm gemini --input <task-file>.");
            String value = args[++i];
            if (value.isBlank() || value.startsWith("--")) throw new IllegalArgumentException("Missing value for " + flag);
            if (flag.equals("--llm")) { if (llm != null) throw new IllegalArgumentException("Duplicate --llm."); llm = value; }
            else { if (input != null) throw new IllegalArgumentException("Duplicate --input."); input = value; }
        }
        if (!"gemini".equals(llm) || input == null) throw new IllegalArgumentException("Specify --llm gemini and --input <task-file>.");
        try { return new Options(false, Path.of(input)); }
        catch (InvalidPathException ex) { throw new IllegalArgumentException("Invalid input path."); }
    }

    static Task readTask(Path path) throws IOException {
        Map<String, String> fields = new HashMap<>();
        for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = raw.strip();
            if (line.startsWith("\uFEFF")) line = line.substring(1).strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int separator = line.indexOf('=');
            if (separator < 1) throw new IllegalArgumentException("Task lines must be key=value.");
            String key = line.substring(0, separator).strip(), value = line.substring(separator + 1).strip();
            if ((!key.equals("action") && !key.equals("eventId")) || value.isEmpty() || fields.putIfAbsent(key, value) != null)
                throw new IllegalArgumentException("Unknown, empty or duplicate task field: " + key);
        }
        if (!"fuse-ideas".equals(fields.get("action")) || !fields.containsKey("eventId"))
            throw new IllegalArgumentException("Task requires action=fuse-ideas and eventId=<saved-event-id>.");
        return new Task(fields.get("eventId"));
    }

    static void dispatch(Task task, EventPlanningFacade facade, PrintStream out) {
        var controller = new EventPlanningController(facade);
        var concepts = controller.fuseIdeas(task.eventId());
        out.println("Saved " + concepts.size() + " generated concepts for event " + task.eventId() + ":");
        for (var concept : concepts) {
            out.println("\n" + concept.id() + " | " + concept.title() + "\n" + concept.summary());
            out.println("Themes: " + concept.themes());
            out.println("Source idea IDs: " + concept.incorporatedIdeas());
            out.println("Conflicts: " + concept.conflicts());
            out.println("Compromises: " + concept.compromises());
            out.println("Uncertainties: " + concept.uncertainties());
        }
    }
    static int execute(String[] args, InputStream in, PrintStream out, PrintStream err) {
        return execute(args, in, out, err, EventPlanningFacade::persistent, () -> GeminiConfiguration.load());
    }
    static int execute(String[] args, InputStream in, PrintStream out, PrintStream err,
            Supplier<EventPlanningFacade> factory, Runnable checkCredentials) {
        final Options options;
        try { options = parse(args); }
        catch (IllegalArgumentException ex) { err.println(ex.getMessage()); err.println("Use --help for usage."); return 2; }
        if (options.help()) { out.print(HELP); return 0; }
        if (options.input() == null) {
            try { CLIApp.run(factory.get(), in, out); return 0; }
            catch (RuntimeException ex) { err.println("Planit operation failed: " + ex.getMessage()); return 5; }
        }
        final Task task;
        try { task = readTask(options.input()); }
        catch (IOException | SecurityException ex) { err.println("Cannot read input task file: " + options.input()); return 3; }
        catch (IllegalArgumentException ex) { err.println("Invalid task: " + ex.getMessage()); return 2; }
        try { checkCredentials.run(); }
        catch (PlanningException ex) {
            err.println(ex.getMessage());
            return ex.getMessage().startsWith("Set GEMINI_API_KEY") ? 4 : 5;
        }
        try (var facade = factory.get()) { dispatch(task, facade, out); return 0; }
        catch (RuntimeException ex) { err.println("Planit operation failed: " + ex.getMessage()); return 5; }
    }
}
