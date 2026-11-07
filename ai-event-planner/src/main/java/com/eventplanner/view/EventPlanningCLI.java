package com.eventplanner.view;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.Event;
import com.eventplanner.model.EventConcept;
import com.eventplanner.model.EventBlueprint;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.strategy.PlanningStrategy;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.function.Function;

/** Text view: reads commands and delegates all application operations to the controller. */
public class EventPlanningCLI {
    private final EventPlanningController controller;
    private final Scanner input;
    private final PrintStream output;

    public EventPlanningCLI(EventPlanningController controller, Scanner input, PrintStream output) {
        this.controller = java.util.Objects.requireNonNull(controller);
        this.input = java.util.Objects.requireNonNull(input);
        this.output = java.util.Objects.requireNonNull(output);
    }

    public void run() {
        output.println("Event planner. Type help for commands.");
        output.println("Events are stored in memory and lost when you exit.");
        try {
            while (true) {
                String command = read("planner> ");
                if (command.isEmpty()) continue;
                String[] parts = command.split("\\s+");
                try {
                    if (command.equals("exit")) break;
                    if (command.equals("help")) {
                        output.println("help | event create | event list | event view <id> | event delete <id> | exit");
                        output.println("ideas add <eventId> | ideas list <eventId> | ideas fuse <eventId> | concepts list <eventId>");
                        output.println("constraints add <eventId> | constraints list <eventId>");
                        output.println("blueprint generate <eventId> <conceptId> | blueprint list <eventId> | blueprint view <eventId> <blueprintId>");
                    } else if (command.equals("event create")) {
                        createEvent();
                    } else if (command.equals("event list")) {
                        var events = controller.listEvents();
                        if (events.isEmpty()) output.println("No events.");
                        events.forEach(this::display);
                    } else if (parts.length == 3 && parts[0].equals("event") && parts[1].equals("view")) {
                        display(controller.viewEvent(parts[2]));
                    } else if (parts.length == 3 && parts[0].equals("event") && parts[1].equals("delete")) {
                        Event event = controller.viewEvent(parts[2]);
                        String confirmation = read("Delete '" + event.getName() + "'? Type yes to confirm: ");
                        if (confirmation.equalsIgnoreCase("yes")) {
                            output.println(controller.deleteEvent(parts[2]) ? "Event deleted." : "Event no longer exists.");
                        } else {
                            output.println("Deletion cancelled.");
                        }
                    } else if (parts.length == 3 && parts[0].equals("ideas") && parts[1].equals("add")) {
                        controller.viewEvent(parts[2]);
                        String organizer = prompt("Organizer name: ", this::nonBlank);
                        String idea = prompt("Idea: ", this::nonBlank);
                        output.println("Idea added. ID: " + controller.addIdea(parts[2], organizer, idea).id());
                    } else if (parts.length == 3 && parts[0].equals("ideas") && parts[1].equals("list")) {
                        var ideas = controller.listIdeas(parts[2]);
                        if (ideas.isEmpty()) output.println("No ideas. Use ideas add <eventId> first.");
                        ideas.forEach(idea -> output.println(idea.id() + " | " + idea.organizer() + ": " + idea.text()));
                    } else if (parts.length == 3 && parts[0].equals("ideas") && parts[1].equals("fuse")) {
                        output.println("Requesting concept alternatives from Gemini...");
                        controller.fuseIdeas(parts[2]).forEach(this::displayConcept);
                    } else if (parts.length == 3 && parts[0].equals("concepts") && parts[1].equals("list")) {
                        var concepts = controller.listConcepts(parts[2]);
                        if (concepts.isEmpty()) output.println("No concepts. Fuse ideas first.");
                        concepts.forEach(this::displayConcept);
                    } else if (parts.length == 3 && parts[0].equals("constraints") && parts[1].equals("add")) {
                        controller.viewEvent(parts[2]);
                        controller.addConstraint(parts[2], prompt("Goal or constraint (resources, accessibility, staffing, etc.): ", this::nonBlank));
                        output.println("Constraint added. Free-text constraints require manual review of proposals.");
                    } else if (parts.length == 3 && parts[0].equals("constraints") && parts[1].equals("list")) {
                        var constraints = controller.listConstraints(parts[2]);
                        if (constraints.isEmpty()) output.println("No additional constraints specified.");
                        constraints.forEach(output::println);
                    } else if (parts.length == 4 && parts[0].equals("blueprint") && parts[1].equals("generate")) {
                        // Validate event/concept before collecting priority or making a paid API call.
                        var concepts = controller.listConcepts(parts[2]);
                        if (concepts.stream().noneMatch(concept -> concept.id().equals(parts[3]))) {
                            throw new IllegalArgumentException("No concept found for this event with ID: " + parts[3]);
                        }
                        String priority = prompt("Priority (budget/community/attendance): ", value -> PlanningStrategy.fromName(value).name());
                        output.println("Requesting blueprint alternatives from Gemini...");
                        controller.generateBlueprints(parts[2], parts[3], priority).forEach(this::displayBlueprint);
                    } else if (parts.length == 3 && parts[0].equals("blueprint") && parts[1].equals("list")) {
                        var blueprints = controller.listBlueprints(parts[2]);
                        if (blueprints.isEmpty()) output.println("No blueprints. Generate blueprints from a concept first.");
                        blueprints.forEach(blueprint -> output.println(blueprint.id() + " | " + blueprint.title()
                                + " | concept: " + blueprint.conceptId() + " | priority: " + blueprint.priority()));
                    } else if (parts.length == 4 && parts[0].equals("blueprint") && parts[1].equals("view")) {
                        displayBlueprint(controller.viewBlueprint(parts[2], parts[3]));
                    } else {
                        output.println("Unknown command or invalid arguments. Type help for usage.");
                    }
                } catch (IllegalArgumentException | PlanningException ex) {
                    output.println("Error: " + ex.getMessage());
                }
            }
        } catch (EndOfInput ex) {
            output.println("\nEnd of input. Any unfinished creation or deletion was cancelled.");
        }
        output.println("Goodbye.");
    }

    private void createEvent() {
        String name = prompt("Name: ", this::nonBlank);
        String type = prompt("Type: ", this::nonBlank);
        String location = prompt("Location: ", this::nonBlank);
        LocalDateTime dateTime = prompt("Date/time (yyyy-MM-ddTHH:mm, e.g. 2026-12-01T18:30): ", value -> {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Enter a valid date/time such as 2026-12-01T18:30.");
            }
        });
        int attendance = prompt("Expected attendance: ", value -> {
            try {
                int parsed = Integer.parseInt(value);
                if (parsed >= 0) return parsed;
            } catch (NumberFormatException ignored) { }
            throw new IllegalArgumentException("Attendance must be a non-negative whole number within the integer range.");
        });
        double budget = prompt("Total budget: ", value -> {
            try {
                double parsed = Double.parseDouble(value);
                if (Double.isFinite(parsed) && parsed >= 0) return parsed;
            } catch (NumberFormatException ignored) { }
            throw new IllegalArgumentException("Budget must be a finite, non-negative number.");
        });
        Event event = controller.createEvent(name, type, location, dateTime, attendance, budget);
        output.println("Event created. ID: " + event.getId());
    }

    private String nonBlank(String value) {
        if (value.isBlank()) throw new IllegalArgumentException("This field is required.");
        return value;
    }

    private <T> T prompt(String label, Function<String, T> parser) {
        while (true) {
            String value = read(label);
            try {
                return parser.apply(value);
            } catch (IllegalArgumentException ex) {
                output.println("Error: " + ex.getMessage());
            }
        }
    }

    private String read(String label) {
        output.print(label);
        output.flush();
        if (!input.hasNextLine()) throw new EndOfInput();
        return input.nextLine().trim();
    }

    private void display(Event event) {
        output.printf(java.util.Locale.ROOT,
                "ID: %s%nName: %s%nType: %s%nLocation: %s%nDate/time: %s%nAttendance: %d%nBudget: %.2f%n%n",
                event.getId(), event.getName(), event.getType(), event.getLocation(),
                event.getDateTime(), event.getExpectedAttendance(), event.getTotalBudget());
    }

    private void displayConcept(EventConcept concept) {
        output.println("Concept ID: " + concept.id() + " | " + concept.title());
        output.println("UNVERIFIED AI proposal: " + concept.summary());
        displayItems("Themes", concept.themes());
        displayItems("Incorporated source idea IDs", concept.incorporatedIdeas());
        displayItems("Conflicts", concept.conflicts());
        displayItems("Compromises", concept.compromises());
        displayItems("Uncertainties", concept.uncertainties());
    }

    private void displayBlueprint(EventBlueprint blueprint) {
        output.println("Blueprint ID: " + blueprint.id() + " | " + blueprint.title());
        output.println("Concept ID: " + blueprint.conceptId() + " | priority: " + blueprint.priority());
        output.println("UNVERIFIED AI proposal: " + blueprint.summary());
        output.printf(java.util.Locale.ROOT, "Hypothetical estimated budget: %.2f | Planned attendance: %d | Required capacity: %d%n",
                blueprint.estimatedBudget(), blueprint.plannedAttendance(), blueprint.requiredCapacity());
        displayItems("Agenda outline", blueprint.agenda());
        displayItems("Venue requirements (unverified)", blueprint.venueRequirements());
        displayItems("Catering assumptions (unverified)", blueprint.cateringAssumptions());
        displayItems("Resources needed", blueprint.resources());
        displayItems("Checklist", blueprint.checklist());
        displayItems("Assumptions", blueprint.assumptions());
        displayItems("Feasibility warnings", blueprint.feasibilityWarnings());
    }

    private void displayItems(String label, java.util.List<String> items) {
        output.println(label + ": " + (items.isEmpty() ? "None reported by model; review manually." : ""));
        items.forEach(item -> output.println("  - " + item));
    }

    private static class EndOfInput extends RuntimeException { }
}
