package com.eventplanner.view;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.Event;
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
                    } else {
                        output.println("Unknown command or invalid arguments. Type help for usage.");
                    }
                } catch (IllegalArgumentException ex) {
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

    private static class EndOfInput extends RuntimeException { }
}
