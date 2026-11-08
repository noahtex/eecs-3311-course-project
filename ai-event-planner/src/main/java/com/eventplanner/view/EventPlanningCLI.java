package com.eventplanner.view;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.Event;
import com.eventplanner.model.EventConcept;
import com.eventplanner.model.EventBlueprint;
import com.eventplanner.model.*;
import java.util.List;
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
                        milestoneHelp();
                        output.println("budget generate/view/validate/approve/discard <eventId> | budget edit <eventId> <itemId>");
                        output.println("venues research/list/compare/add <eventId>");
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
                    } else if (commercialCommand(parts)) {
                        // F05/F06 use the shared controller/facade.
                    } else if (milestoneCommand(parts)) {
                        // F03/F04 command handled through the same controller.
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

    private void milestoneHelp() {
        output.println("team set <eventId> | team list <eventId> | speaker set <eventId> | resource set <eventId>");
        output.println("tasks generate <eventId> | tasks list <eventId> | tasks assign <eventId> <taskId> | tasks status <eventId> <taskId>");
        output.println("tasks approve <eventId> | tasks discard <eventId>");
        output.println("schedule window <eventId> | schedule build <eventId> | schedule view <eventId> | schedule conflicts <eventId>");
        output.println("schedule edit <eventId> <sessionId> | schedule approve <eventId> | schedule discard <eventId>");
        output.println("Proposals and edits remain drafts until explicitly approved. Times use the event's local timezone.");
    }

    private boolean commercialCommand(String[] parts) {
        if (parts.length == 4 && parts[0].equals("budget") && parts[1].equals("edit")) {
            String eventId = parts[2];
            BudgetItem original = controller.viewBudget(eventId).items().stream().filter(item -> item.id().equals(parts[3]))
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown budget item ID."));
            output.println("Current item: " + original);
            String category = prompt("Category: ", this::nonBlank);
            String description = prompt("Description: ", this::nonBlank);
            var quantity = moneyPrompt("Quantity (positive): ", true);
            var status = prompt("Price status (CONFIRMED/ESTIMATED/UNKNOWN): ", value -> parseEnum(BudgetItem.PriceStatus.class, value));
            var cost = status == BudgetItem.PriceStatus.UNKNOWN ? null : moneyPrompt("Unit cost (non-negative): ", false);
            String source = prompt("Quote reference/source or explicit estimate assumption: ", this::nonBlank);
            controller.editBudget(eventId, new BudgetItem(original.id(), category, description, quantity, cost, status, source));
            showBudget(eventId);
            output.println("Edit retained as draft. Use budget approve to save or budget discard to cancel.");
            return true;
        }
        if (parts.length != 3) return false;
        String eventId = parts[2];
        switch (parts[0] + " " + parts[1]) {
            case "budget generate" -> {
                controller.viewEvent(eventId);
                String currency = prompt("Currency for the existing event spending limit (e.g. CAD): ", value -> {
                    String normalized = value.toUpperCase(java.util.Locale.ROOT);
                    java.util.Currency.getInstance(normalized); return normalized;
                });
                List<String> dietary = csv(read("Dietary requirements (comma-separated; blank = unspecified): "));
                String catering = prompt("Catering needs (or explicitly none): ", this::nonBlank);
                String priority = prompt("Priority (budget/community/attendance): ", value -> PlanningStrategy.fromName(value).name());
                controller.configureBudgetNeeds(eventId, currency, dietary, catering);
                controller.generateBudget(eventId, priority);
                showBudget(eventId);
                output.println("Proposal retained as draft. Review/edit, then budget approve.");
            }
            case "budget view" -> showBudget(eventId);
            case "budget validate" -> { showBudget(eventId); }
            case "budget approve" -> {
                showBudget(eventId);
                controller.approveBudget(eventId);
                output.println("Budget approved and stored; estimates remain estimates, not confirmed prices.");
            }
            case "budget discard" -> { controller.discardBudget(eventId); output.println("Budget draft discarded; approved budget retained."); }
            case "venues research" -> {
                controller.viewEvent(eventId);
                String query = prompt("Venue search (type, area, requirements): ", this::nonBlank);
                var result = controller.researchVenues(eventId, query);
                result.options().forEach(this::showVenue);
                showIssues(result.warnings());
                if (result.options().isEmpty()) output.println("No candidates returned. Revise search or use venues add.");
                output.println("Candidates stored for research only; no venue is selected or booked.");
            }
            case "venues list" -> {
                var venues = controller.listVenues(eventId);
                if (venues.isEmpty()) output.println("No venues. Use venues research or venues add.");
                venues.forEach(this::showVenue);
            }
            case "venues compare" -> {
                controller.viewEvent(eventId);
                if (controller.listVenues(eventId).isEmpty()) throw new IllegalArgumentException("No venues. Use venues research or venues add.");
                boolean wheelchair = prompt("Require wheelchair-accessible entrance? (yes/no): ", this::yesNo);
                List<String> equipment = csv(read("Required equipment (comma-separated; blank = unspecified): "));
                controller.configureVenueNeeds(eventId, wheelchair, equipment);
                controller.listVenues(eventId).forEach(this::showVenue);
                var comparisons = controller.compareVenues(eventId);
                int rank = 1;
                for (var comparison : comparisons) {
                    output.println("Rank " + rank++ + " | " + comparison.venueId() + " | known-fit score=" + comparison.knownFitScore());
                    output.println("UNVERIFIED AI interpretation: " + comparison.analysis());
                    displayItems("AI trade-offs (unverified)", comparison.tradeOffs());
                    showIssues(comparison.warnings());
                }
                output.println("Ranking considers represented facts only; unknowns do not establish suitability. No booking made.");
            }
            case "venues add" -> addManualVenue(eventId);
            default -> { return false; }
        }
        return true;
    }

    private void showBudget(String eventId) {
        Budget budget = controller.viewBudget(eventId);
        output.println((controller.hasBudgetDraft(eventId) ? "DRAFT" : "APPROVED") + " budget " + budget.id()
                + " | currency=" + budget.currency() + " | priority=" + budget.priority());
        budget.items().forEach(item -> output.println(item.id() + " | " + item.category() + " | " + item.description()
                + " | quantity=" + item.quantity() + " | unit cost=" + (item.unitCost() == null ? "UNKNOWN" : item.unitCost())
                + " | total=" + (item.total() == null ? "UNKNOWN" : item.total()) + " | " + item.priceStatus() + " | source=" + item.source()));
        output.println("Known subtotal (includes estimates; excludes UNKNOWN amounts): " + controller.budgetTotal(eventId) + " " + budget.currency());
        Event event = controller.viewEvent(eventId);
        output.println("Unchanged event spending limit: " + event.getTotalBudget() + " " + event.getPlanningCurrency());
        if (budget.currency().equals(event.getPlanningCurrency())) output.println("Provisional remaining / negative shortfall: " + controller.budgetRemaining(eventId));
        budget.providers().forEach(this::showVenue);
        budget.catering().forEach(option -> {
            output.println("UNVERIFIED catering recommendation | provider=" + (option.providerId() == null ? "generic plan; no provider verified" : option.providerId())
                    + " | " + option.recommendation());
            displayItems("Dietary notes (confirm with provider)", option.dietaryNotes());
        });
        displayItems("Assumptions", budget.assumptions());
        showIssues(controller.validateBudget(eventId));
    }
    private void showVenue(VenueOption venue) {
        output.println("Venue/provider ID: " + venue.id() + " | " + venue.name() + " | address=" + venue.address());
        output.println("Source: " + venue.source() + " | retrieved/entered=" + venue.retrievedAt() + " | attributes=" + venue.providerAttributes());
        output.println("Capacity=" + venue.capacity() + " | rental cost=" + venue.rentalCost() + " " + venue.currency());
        output.println("Wheelchair entrance=" + venue.wheelchairEntrance() + " | equipment=" + venue.equipment());
        output.println("Event availability UNKNOWN. Provider-reported and planner-entered information require appropriate confirmation.");
    }
    private void addManualVenue(String eventId) {
        Event event = controller.viewEvent(eventId);
        String name = prompt("Venue name: ", this::nonBlank);
        String address = prompt("Address/location: ", this::nonBlank);
        String source = prompt("Source/reference for your information (e.g. quote or unverified note): ", this::nonBlank);
        Evidence.Status status = prompt("Status of entered attributes (PLANNER_CONFIRMED/ESTIMATED): ", value -> {
            var parsed = parseEnum(Evidence.Status.class, value);
            if (parsed != Evidence.Status.PLANNER_CONFIRMED && parsed != Evidence.Status.ESTIMATED)
                throw new IllegalArgumentException("Choose PLANNER_CONFIRMED or ESTIMATED; blank attributes remain UNKNOWN.");
            return parsed;
        });
        Integer capacity = prompt("Capacity (blank = UNKNOWN): ", value -> {
            if (value.isBlank()) return null;
            try { int parsed = Integer.parseInt(value); if (parsed > 0) return parsed; } catch (NumberFormatException ignored) { }
            throw new IllegalArgumentException("Enter a positive capacity or leave blank.");
        });
        java.math.BigDecimal cost = prompt("Total event rental cost in " + event.getPlanningCurrency() + " (blank = UNKNOWN): ", value ->
                value.isBlank() ? null : parseMoney(value, false));
        Boolean wheelchair = prompt("Wheelchair entrance (yes/no; blank = UNKNOWN): ", value -> value.isBlank() ? null : yesNo(value));
        String equipmentInput = read("Known equipment (comma-separated; blank = UNKNOWN; type none for an explicitly empty list): ");
        List<String> equipment = equipmentInput.isBlank() ? null : equipmentInput.equalsIgnoreCase("none") ? List.of() : csv(equipmentInput);
        VenueOption venue = new VenueOption("manual:" + java.util.UUID.randomUUID(), name, address, "Manual planner entry: " + source,
                java.time.Instant.now(), java.util.Map.of(), manualEvidence(capacity, status, source), manualEvidence(cost, status, source),
                event.getPlanningCurrency(), manualEvidence(wheelchair, status, source), manualEvidence(equipment, status, source));
        showVenue(venue);
        if (read("Store this venue candidate? Type yes: ").equalsIgnoreCase("yes")) {
            controller.addVenue(eventId, venue); output.println("Manual venue candidate stored; no booking made.");
        } else output.println("Manual entry cancelled.");
    }
    private <T> Evidence<T> manualEvidence(T value, Evidence.Status status, String source) {
        return value == null ? Evidence.unknown() : new Evidence<>(value, status, source);
    }
    private boolean yesNo(String value) {
        if (value.equalsIgnoreCase("yes")) return true;
        if (value.equalsIgnoreCase("no")) return false;
        throw new IllegalArgumentException("Enter yes or no.");
    }
    private java.math.BigDecimal moneyPrompt(String label, boolean positive) { return prompt(label, value -> parseMoney(value, positive)); }
    private java.math.BigDecimal parseMoney(String value, boolean positive) {
        try {
            var amount = new java.math.BigDecimal(value);
            if (amount.signum() >= 0 && (!positive || amount.signum() > 0)) return amount;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(positive ? "Enter a positive quantity." : "Enter a non-negative decimal amount.");
    }

    private boolean milestoneCommand(String[] parts) {
        if (parts.length < 3 || parts.length > 4) return false;
        String operation = parts[0] + " " + parts[1];
        String eventId = parts[2];
        if (parts.length == 4) {
            switch (operation) {
                case "tasks assign" -> {
                    Task original = controller.listTasks(eventId).stream().filter(task -> task.id().equals(parts[3])).findFirst()
                            .orElseThrow(() -> new IllegalArgumentException("Unknown task ID."));
                    controller.listMembers(eventId).forEach(user -> output.println(user.getId() + " | " + user.getName()));
                    String user = read("Assigned member ID (blank = unassigned): ");
                    LocalDateTime start = datePrompt("Work start (ISO local datetime; current " + original.workStart() + "): ");
                    LocalDateTime due = datePrompt("Deadline (ISO local datetime; current " + original.deadline() + "): ");
                    controller.reviseTask(eventId, parts[3], user.isBlank() ? null : user, start, due);
                    showTasks(eventId);
                    reviewTasks(eventId);
                }
                case "tasks status" -> {
                    if (controller.listTasks(eventId).stream().noneMatch(task -> task.id().equals(parts[3])))
                        throw new IllegalArgumentException("Unknown task ID.");
                    Task.Status status = prompt("Status (TODO/IN_PROGRESS/DONE): ", value -> parseEnum(Task.Status.class, value));
                    controller.updateTaskStatus(eventId, parts[3], status);
                    showTasks(eventId);
                    reviewTasks(eventId);
                }
                case "schedule edit" -> {
                    Session original = controller.viewSchedule(eventId).sessions().stream().filter(session -> session.id().equals(parts[3]))
                            .findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown session ID."));
                    output.println("Editing: " + original);
                    String title = prompt("Title: ", this::nonBlank);
                    Session.Kind kind = prompt("Kind (SETUP/ACTIVITY/BREAK/TEARDOWN): ", value -> parseEnum(Session.Kind.class, value));
                    TimeSlot slot = slotPrompt();
                    String speaker = read("Speaker ID (blank = no speaker): ");
                    String space = prompt("Space resource ID: ", this::nonBlank);
                    List<String> resources = csv(read("Other exclusive resource IDs (comma-separated; blank = none): "));
                    controller.reviseSession(eventId, new Session(parts[3], title, kind, slot, speaker.isBlank() ? null : speaker, space, resources));
                    showSchedule(eventId);
                    reviewSchedule(eventId);
                }
                default -> { return false; }
            }
            return true;
        }
        switch (operation) {
            case "team set" -> {
                controller.viewEvent(eventId);
                String id = prompt("Stable member ID (existing ID replaces profile): ", this::nonBlank);
                String name = prompt("Member name: ", this::nonBlank);
                List<String> skills = csv(read("Skills (comma-separated): "));
                List<String> preferences = csv(read("Preferred tasks/topics (comma-separated): "));
                List<TimeSlot> availability = availabilityPrompt();
                double workload = hoursPrompt("Existing workload hours outside stored event tasks: ");
                double capacity = hoursPrompt("Total capacity hours for the planning horizon: ");
                User user = new User(id, name, "", "Planner");
                user.configurePlanning(skills, preferences, availability, workload, capacity);
                controller.configureMember(eventId, user);
                output.println("Member profile saved. Revalidate existing task plans after profile changes.");
            }
            case "team list" -> controller.listMembers(eventId).forEach(user -> output.println(user.getId() + " | " + user.getName()
                    + " | skills=" + user.getSkills() + " | preferences=" + user.getPreferences()
                    + " | availability=" + user.getAvailability() + " | existing hours=" + user.getExistingWorkloadHours()
                    + " | capacity hours=" + user.getCapacityHours()));
            case "speaker set" -> {
                controller.viewEvent(eventId);
                String id = prompt("Stable speaker ID (existing ID replaces profile): ", this::nonBlank);
                String name = prompt("Speaker name: ", this::nonBlank);
                controller.configureSpeaker(eventId, new Speaker(id, name, availabilityPrompt()));
                output.println("Speaker saved. Check schedule conflicts after availability changes.");
            }
            case "resource set" -> {
                controller.viewEvent(eventId);
                String resource = prompt("Space or exclusive resource ID (existing ID replaces availability): ", this::nonBlank);
                controller.configureResource(eventId, resource, availabilityPrompt());
                output.println("Resource saved. Check schedule conflicts after availability changes.");
            }
            case "tasks generate" -> {
                controller.generateTasks(eventId);
                showTasks(eventId);
                reviewTasks(eventId);
            }
            case "tasks list" -> showTasks(eventId);
            case "tasks approve" -> { controller.approveTasks(eventId); output.println("Task plan approved and stored."); }
            case "tasks discard" -> { controller.discardTasks(eventId); output.println("Task draft discarded; approved plan retained."); }
            case "schedule window" -> {
                Event event = controller.viewEvent(eventId);
                output.println("Event window starts " + event.getDateTime() + "; include setup and teardown within the window.");
                controller.configureEventEnd(eventId, datePrompt("Event end (ISO local datetime): "));
                output.println("Event end saved. Check existing schedule conflicts.");
            }
            case "schedule build" -> {
                controller.buildSchedule(eventId);
                showSchedule(eventId);
                reviewSchedule(eventId);
            }
            case "schedule view" -> showSchedule(eventId);
            case "schedule conflicts" -> showIssues(controller.scheduleConflicts(eventId));
            case "schedule approve" -> { controller.approveSchedule(eventId); output.println("Schedule approved and stored."); }
            case "schedule discard" -> { controller.discardSchedule(eventId); output.println("Schedule draft discarded; approved schedule retained."); }
            default -> { return false; }
        }
        return true;
    }

    private void showTasks(String eventId) {
        output.println(controller.hasTaskDraft(eventId) ? "DRAFT task plan (not approved)" : "APPROVED tasks");
        var tasks = controller.listTasks(eventId);
        if (tasks.isEmpty()) output.println("No tasks. Use tasks generate <eventId>.");
        tasks.forEach(task -> output.println(task.id() + " | " + task.title() + " | " + task.description()
                + " | assignee=" + (task.assignedUserId() == null ? "UNASSIGNED" : task.assignedUserId())
                + " | start=" + task.workStart() + " | deadline=" + task.deadline() + " | hours=" + task.effortHours()
                + " | skills=" + task.requiredSkills() + " | " + task.priority() + " | " + task.status()));
        showIssues(controller.taskWarnings(eventId));
    }
    private void showSchedule(String eventId) {
        Schedule schedule = controller.viewSchedule(eventId);
        output.println((controller.hasScheduleDraft(eventId) ? "DRAFT" : "APPROVED") + " schedule " + schedule.id());
        schedule.sessions().stream().sorted(java.util.Comparator.comparing(session -> session.slot().start()))
                .forEach(session -> output.println(session.id() + " | " + session.title() + " | " + session.kind()
                        + " | " + session.slot() + " | speaker=" + session.speakerId() + " | space=" + session.space()
                        + " | resources=" + session.resources()));
        showIssues(controller.scheduleConflicts(eventId));
    }
    private void showIssues(List<String> issues) {
        if (issues.isEmpty()) output.println("No deterministic issues detected; review event constraints manually.");
        issues.forEach(issue -> output.println("WARNING: " + issue));
    }
    private void reviewTasks(String eventId) {
        if (read("Approve task draft? Type yes; otherwise keep draft for revision: ").equalsIgnoreCase("yes")) {
            controller.approveTasks(eventId);
            output.println("Task plan approved and stored.");
        } else output.println("Draft retained. Use tasks assign/status, then tasks approve; or tasks discard.");
    }
    private void reviewSchedule(String eventId) {
        if (read("Approve schedule draft? Type yes; otherwise keep draft for revision: ").equalsIgnoreCase("yes")) {
            controller.approveSchedule(eventId);
            output.println("Schedule approved and stored.");
        } else output.println("Draft retained. Use schedule edit, then schedule approve; or schedule discard.");
    }
    private LocalDateTime datePrompt(String label) {
        return prompt(label, value -> {
            try { return LocalDateTime.parse(value); }
            catch (DateTimeParseException ex) { throw new IllegalArgumentException("Enter an ISO local datetime, e.g. 2026-12-01T18:30."); }
        });
    }
    private TimeSlot slotPrompt() {
        while (true) {
            LocalDateTime start = datePrompt("Availability/session start (ISO local datetime): ");
            LocalDateTime end = datePrompt("Availability/session end (ISO local datetime): ");
            try { return new TimeSlot(start, end); }
            catch (IllegalArgumentException ex) { output.println("Error: " + ex.getMessage()); }
        }
    }
    private List<TimeSlot> availabilityPrompt() {
        var slots = new java.util.ArrayList<TimeSlot>();
        while (read("Add an availability window? Type yes (none means UNKNOWN): ").equalsIgnoreCase("yes")) slots.add(slotPrompt());
        return List.copyOf(slots);
    }
    private double hoursPrompt(String label) {
        return prompt(label, value -> {
            try { double hours = Double.parseDouble(value); if (Double.isFinite(hours) && hours >= 0) return hours; }
            catch (NumberFormatException ignored) { }
            throw new IllegalArgumentException("Enter finite non-negative hours.");
        });
    }
    private List<String> csv(String value) {
        return value.isBlank() ? List.of() : java.util.Arrays.stream(value.split(",")).map(String::strip)
                .map(item -> PlanningValues.text(item, "List entry")).toList();
    }
    private <T extends Enum<T>> T parseEnum(Class<T> type, String value) {
        try { return Enum.valueOf(type, value.toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("Choose one of " + java.util.Arrays.toString(type.getEnumConstants())); }
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
