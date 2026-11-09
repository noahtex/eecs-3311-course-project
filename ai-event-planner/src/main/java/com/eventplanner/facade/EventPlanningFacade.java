package com.eventplanner.facade;

import com.eventplanner.model.Event;
import com.eventplanner.model.EventIdea;
import com.eventplanner.model.EventConcept;
import com.eventplanner.model.EventBlueprint;
import com.eventplanner.model.*;
import com.eventplanner.service.TaskService;
import com.eventplanner.service.ScheduleService;
import com.eventplanner.service.BudgetService;
import com.eventplanner.tool.*;
import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.GeminiAdapter;
import com.eventplanner.ai.GeminiAPIClient;
import com.eventplanner.strategy.PlanningStrategy;
import com.eventplanner.service.EventService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Application operations and validation over the existing event service. */
public class EventPlanningFacade implements AutoCloseable {
    private boolean closed;
    /** Release every owned network resource, even if another resource's close fails. */
    @Override public synchronized void close() {
        if (closed) return;
        closed = true;
        try (agent; venueTool; cateringTool) {
            // Reverse construction order: tools first, then the agent/model.
        }
    }
    private final com.eventplanner.service.AdaptivePlanningService adaptive;
    private Event observed(String id) {
        Event event=viewEvent(id); event.addObserver(taskService); event.addObserver(scheduleService); event.addObserver(budgetService); return event;
    }
    public PlanChange proposeChange(String eventId, String request) { return adaptive.propose(observed(eventId),request); }
    public PlanChange proposeChangeFields(String eventId, java.util.Map<String,String> fields) { return adaptive.proposeFields(observed(eventId),fields); }
    public PlanChange pendingChanges(String eventId) { return adaptive.changes(viewEvent(eventId)); }
    public void confirmChange(String eventId, boolean confirmed) { adaptive.confirm(observed(eventId),confirmed); }
    public ScenarioResult simulateScenario(String eventId, String request) { return adaptive.simulate(viewEvent(eventId),request); }
    public ContingencyPlan generateContingency(String eventId, String risk) { return adaptive.contingency(viewEvent(eventId),risk); }
    public List<ContingencyPlan> listContingencies(String eventId) { return viewEvent(eventId).getContingencies(); }
    public Communication generateCommunication(String eventId, Communication.Channel channel, String tone) { return adaptive.communication(viewEvent(eventId),channel,tone); }
    public List<Communication> listCommunications(String eventId) { return viewEvent(eventId).getCommunications(); }
    public Communication viewCommunication(String eventId, String id) { return adaptive.findCommunication(viewEvent(eventId),id); }
    public Communication editCommunication(String eventId, String id, String body) { return adaptive.editCommunication(viewEvent(eventId),id,body); }
    public Communication approveCommunication(String eventId, String id) { return adaptive.approveCommunication(viewEvent(eventId),id); }
    public String askFaq(String eventId, String question) { return adaptive.faq(viewEvent(eventId),question); }
    public void recordPostEvent(String eventId, PostEventData data) { adaptive.record(viewEvent(eventId),data); }
    public PostEventReport analyzePostEvent(String eventId) { return adaptive.analyze(viewEvent(eventId)); }
    public List<PostEventReport> viewPostEventReports(String eventId) { return viewEvent(eventId).getReports(); }
    private final EventService eventService;
    private final EventPlanningAgent agent;
    private final AITool venueTool;
    private final AITool cateringTool;
    private final BudgetService budgetService = new BudgetService();
    private final java.util.Map<Event, Budget> budgetDrafts = new java.util.HashMap<>();

    public void configureBudgetNeeds(String eventId, String currency, List<String> dietary, String catering) {
        viewEvent(eventId).configureBudgetNeeds(currency, dietary, catering);
    }
    public void configureVenueNeeds(String eventId, boolean wheelchair, List<String> equipment) {
        viewEvent(eventId).configureVenueNeeds(wheelchair, equipment);
    }
    public Budget generateBudget(String eventId, String priority) {
        Event event = viewEvent(eventId);
        agent.setPlanningStrategy(PlanningStrategy.fromName(priority));
        ToolResult providers;
        try {
            providers = cateringTool.execute("catering providers near " + event.getLocation() + " for " + event.getType()
                    + " " + String.join(" ", event.getDietaryRequirements()));
        } catch (com.eventplanner.ai.PlanningException ex) {
            providers = new ToolResult("Unavailable", List.of(), List.of(
                    "Live catering-provider discovery unavailable. Configure GOOGLE_PLACES_API_KEY / Places API (New), billing and network, or confirm providers manually. Recommendations and all costs remain unverified estimates."));
        }
        Budget proposed = agent.planBudgetAndCatering(event, providers);
        budgetDrafts.put(event, proposed);
        return proposed;
    }
    public boolean hasBudgetDraft(String eventId) { return budgetDrafts.containsKey(viewEvent(eventId)); }
    public Budget viewBudget(String eventId) {
        Event event = viewEvent(eventId);
        Budget budget = budgetDrafts.getOrDefault(event, event.getBudget());
        if (budget == null) throw new IllegalArgumentException("No budget. Use budget generate first.");
        return budget;
    }
    public void editBudget(String eventId, BudgetItem replacement) {
        budgetDrafts.put(viewEvent(eventId), budgetService.updateAllocation(viewBudget(eventId), replacement));
    }
    public List<String> validateBudget(String eventId) { return budgetService.validateBudget(viewEvent(eventId), viewBudget(eventId)); }
    public java.math.BigDecimal budgetTotal(String eventId) { return budgetService.totalExpenses(viewBudget(eventId)); }
    public java.math.BigDecimal budgetRemaining(String eventId) { return budgetService.remainingBudget(viewEvent(eventId), viewBudget(eventId)); }
    public void approveBudget(String eventId) {
        Event event = viewEvent(eventId);
        if (!budgetDrafts.containsKey(event)) throw new IllegalArgumentException("No budget draft to approve.");
        budgetService.saveBudget(event, budgetDrafts.get(event));
        budgetDrafts.remove(event);
    }
    public void discardBudget(String eventId) { budgetDrafts.remove(viewEvent(eventId)); }
    public ToolResult researchVenues(String eventId, String query) {
        Event event = viewEvent(eventId);
        ToolResult result = venueTool.execute(required(query, "Venue search query") + " near " + event.getLocation());
        event.addVenueOptions(result.options());
        return result;
    }
    public List<VenueOption> listVenues(String eventId) { return viewEvent(eventId).getVenueOptions(); }
    public void addVenue(String eventId, VenueOption venue) { viewEvent(eventId).addVenueOptions(List.of(Objects.requireNonNull(venue))); }
    public List<VenueComparison> compareVenues(String eventId) {
        Event event = viewEvent(eventId);
        var comparisons = agent.compareVenues(event);
        event.storeVenueComparisons(comparisons);
        return comparisons;
    }
    private final TaskService taskService = new TaskService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final java.util.Map<Event, List<Task>> taskDrafts = new java.util.HashMap<>();
    private final java.util.Map<Event, Schedule> scheduleDrafts = new java.util.HashMap<>();
    private final java.util.Map<Event, List<String>> proposalWarnings = new java.util.HashMap<>();

    private List<Task> otherTasks(Event event) {
        return eventService.getAllEvents().stream().filter(other -> other != event)
                .flatMap(other -> other.getTasks().stream()).toList();
    }

    public void configureMember(String eventId, User user) {
        PlanningValues.text(user.getId(), "Member ID");
        PlanningValues.text(user.getName(), "Member name");
        var team = viewEvent(eventId).getPlanningTeam();
        team.getMembers().stream().filter(member -> member.getId().equals(user.getId())).findFirst().ifPresent(team::removeMember);
        team.addMember(user);
    }
    public List<User> listMembers(String eventId) { return viewEvent(eventId).getPlanningTeam().getMembers(); }
    public void configureEventEnd(String eventId, LocalDateTime end) { viewEvent(eventId).setEndDateTime(end); }
    public void configureSpeaker(String eventId, Speaker speaker) { viewEvent(eventId).putSpeaker(speaker); }
    public void configureResource(String eventId, String resource, List<TimeSlot> slots) { viewEvent(eventId).putResourceAvailability(resource, slots); }

    public List<Task> generateTasks(String eventId) {
        Event event = viewEvent(eventId);
        var proposed = agent.proposeTaskAssignments(event, otherTasks(event));
        var issues = taskService.validateAssignments(event, proposed, otherTasks(event));
        // Never keep an invented or unsuitable assignee. Preserve the original diagnostics for review.
        var safe = proposed.stream().map(task -> task.assignedUserId() != null
                && issues.stream().anyMatch(issue -> issue.startsWith("Task " + task.id() + " ("))
                ? task.assign(null) : task).toList();
        taskDrafts.put(event, safe);
        proposalWarnings.put(event, issues);
        return safe;
    }
    public boolean hasTaskDraft(String eventId) { return taskDrafts.containsKey(viewEvent(eventId)); }
    public List<Task> listTasks(String eventId) {
        Event event = viewEvent(eventId);
        return taskDrafts.getOrDefault(event, event.getTasks());
    }
    public List<String> taskWarnings(String eventId) {
        Event event = viewEvent(eventId);
        var warnings = new java.util.ArrayList<>(proposalWarnings.getOrDefault(event, List.of()));
        warnings.addAll(taskService.warnings(event, listTasks(eventId), otherTasks(event)));
        return warnings.stream().distinct().toList();
    }
    public void approveTasks(String eventId) {
        Event event = viewEvent(eventId);
        if (!taskDrafts.containsKey(event)) throw new IllegalArgumentException("No task draft to approve.");
        taskService.saveAssignments(event, taskDrafts.get(event), otherTasks(event));
        taskDrafts.remove(event);
        proposalWarnings.remove(event);
    }
    public void discardTasks(String eventId) {
        Event event = viewEvent(eventId);
        taskDrafts.remove(event);
        proposalWarnings.remove(event);
    }
    public void reviseTask(String eventId, String taskId, String memberId, LocalDateTime start, LocalDateTime deadline) {
        Event event = viewEvent(eventId);
        var revised = replaceTask(listTasks(eventId), taskId, task -> task.revise(memberId, start, deadline));
        taskDrafts.put(event, revised);
        proposalWarnings.remove(event);
    }
    public void updateTaskStatus(String eventId, String taskId, Task.Status status) {
        Event event = viewEvent(eventId);
        var revised = replaceTask(listTasks(eventId), taskId, task -> task.withStatus(status));
        // Status edits also require approval; invalid assignments cannot be silently stored.
        taskDrafts.put(event, revised);
    }
    private List<Task> replaceTask(List<Task> tasks, String id, java.util.function.UnaryOperator<Task> update) {
        if (tasks.stream().noneMatch(task -> task.id().equals(id))) throw new IllegalArgumentException("Unknown task ID.");
        return tasks.stream().map(task -> task.id().equals(id) ? update.apply(task) : task).toList();
    }
    public Schedule buildSchedule(String eventId) {
        Event event = viewEvent(eventId);
        Schedule proposed = agent.proposeSchedule(event);
        scheduleDrafts.put(event, proposed);
        return proposed;
    }
    public boolean hasScheduleDraft(String eventId) { return scheduleDrafts.containsKey(viewEvent(eventId)); }
    public Schedule viewSchedule(String eventId) {
        Event event = viewEvent(eventId);
        Schedule schedule = scheduleDrafts.getOrDefault(event, event.getSchedule());
        if (schedule == null) throw new IllegalArgumentException("No schedule. Build one first.");
        return schedule;
    }
    public List<String> scheduleConflicts(String eventId) { return scheduleService.detectConflicts(viewEvent(eventId), viewSchedule(eventId)); }
    public void reviseSession(String eventId, Session revised) {
        Schedule original = viewSchedule(eventId);
        if (original.sessions().stream().noneMatch(session -> session.id().equals(revised.id())))
            throw new IllegalArgumentException("Unknown session ID.");
        scheduleDrafts.put(viewEvent(eventId), new Schedule(original.id(), original.sessions().stream()
                .map(session -> session.id().equals(revised.id()) ? revised : session).toList()));
    }
    public void approveSchedule(String eventId) {
        Event event = viewEvent(eventId);
        if (!scheduleDrafts.containsKey(event)) throw new IllegalArgumentException("No schedule draft to approve.");
        scheduleService.saveSchedule(event, scheduleDrafts.get(event));
        scheduleDrafts.remove(event);
    }
    public void discardSchedule(String eventId) { scheduleDrafts.remove(viewEvent(eventId)); }

    public EventPlanningFacade(EventService eventService) {
        this(eventService, new EventPlanningAgent(new GeminiAdapter(new GeminiAPIClient())));
    }

    public EventPlanningFacade(EventService eventService, EventPlanningAgent agent) {
        this(eventService, agent, new VenueSearchTool(new PlacesAPIClient()), new CateringSearchTool(new PlacesAPIClient()));
    }

    public EventPlanningFacade(EventService eventService, EventPlanningAgent agent, AITool venueTool, AITool cateringTool) {
        this.eventService = Objects.requireNonNull(eventService, "Event service is required.");
        this.agent = Objects.requireNonNull(agent, "Planning agent is required.");
        this.adaptive = new com.eventplanner.service.AdaptivePlanningService(agent);
        this.venueTool = Objects.requireNonNull(venueTool);
        this.cateringTool = Objects.requireNonNull(cateringTool);
    }

    public Event createEvent(String name, String type, String location,
            LocalDateTime dateTime, int attendance, double budget) {
        name = required(name, "Name");
        type = required(type, "Type");
        location = required(location, "Location");
        if (dateTime == null) {
            throw new IllegalArgumentException("Date/time is required.");
        }
        if (attendance < 0) {
            throw new IllegalArgumentException("Attendance must be a non-negative whole number.");
        }
        if (!Double.isFinite(budget) || budget < 0) {
            throw new IllegalArgumentException("Budget must be a finite, non-negative number.");
        }
        Event event = new Event(UUID.randomUUID().toString(), name, type, location,
                dateTime, attendance, budget);
        eventService.createEvent(event);
        return event;
    }

    public List<Event> listEvents() {
        return eventService.getAllEvents();
    }

    public Event viewEvent(String id) {
        Event event = eventService.getEvent(required(id, "Event ID"));
        if (event == null) {
            throw new IllegalArgumentException("No event found with ID: " + id);
        }
        return event;
    }

    public boolean deleteEvent(String id) {
        String checked = required(id, "Event ID");
        Event event = eventService.getEvent(checked);
        boolean deleted = eventService.deleteEvent(checked);
        if (deleted) {
            taskDrafts.remove(event);
            scheduleDrafts.remove(event);
            proposalWarnings.remove(event);
            budgetDrafts.remove(event);
        }
        return deleted;
    }

    public EventIdea addIdea(String eventId, String organizer, String idea) { return eventService.addIdea(eventId, organizer, idea); }
    public List<EventIdea> listIdeas(String eventId) { return eventService.listIdeas(eventId); }
    public void addConstraint(String eventId, String constraint) { eventService.addConstraint(eventId, constraint); }
    public List<String> listConstraints(String eventId) { return eventService.listConstraints(eventId); }
    public List<EventConcept> listConcepts(String eventId) { return eventService.listConcepts(eventId); }

    public List<EventConcept> fuseIdeas(String eventId) {
        var concepts = agent.synthesizeIdeas(eventService.requireEvent(eventId));
        eventService.storeConcepts(eventId, concepts);
        return concepts;
    }

    public List<EventBlueprint> generateBlueprints(String eventId, String conceptId, String priority) {
        Event event = eventService.requireEvent(eventId);
        EventConcept concept = eventService.getConcept(eventId, conceptId);
        agent.setPlanningStrategy(PlanningStrategy.fromName(priority));
        var blueprints = agent.generateBlueprints(event, concept);
        eventService.storeBlueprints(eventId, blueprints);
        return blueprints;
    }

    public List<EventBlueprint> listBlueprints(String eventId) { return eventService.listBlueprints(eventId); }
    public EventBlueprint viewBlueprint(String eventId, String blueprintId) { return eventService.getBlueprint(eventId, blueprintId); }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }
}
