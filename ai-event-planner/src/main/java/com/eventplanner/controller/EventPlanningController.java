package com.eventplanner.controller;

import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.Event;
import com.eventplanner.model.EventIdea;
import com.eventplanner.model.EventConcept;
import com.eventplanner.model.EventBlueprint;
import com.eventplanner.model.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** UI-independent controller shared by application views. */
public class EventPlanningController {
    public PlanChange proposeChange(String eventId, String request) { return facade.proposeChange(eventId,request); }
    public PlanChange proposeChangeFields(String eventId, java.util.Map<String,String> fields) { return facade.proposeChangeFields(eventId,fields); }
    public PlanChange pendingChanges(String eventId) { return facade.pendingChanges(eventId); }
    public void confirmChange(String eventId, boolean confirmed) { facade.confirmChange(eventId,confirmed); }
    public ScenarioResult simulateScenario(String eventId, String request) { return facade.simulateScenario(eventId,request); }
    public ContingencyPlan generateContingency(String eventId, String risk) { return facade.generateContingency(eventId,risk); }
    public List<ContingencyPlan> listContingencies(String eventId) { return facade.listContingencies(eventId); }
    public Communication generateCommunication(String eventId, Communication.Channel channel, String tone) { return facade.generateCommunication(eventId,channel,tone); }
    public List<Communication> listCommunications(String eventId) { return facade.listCommunications(eventId); }
    public Communication viewCommunication(String eventId, String id) { return facade.viewCommunication(eventId,id); }
    public Communication editCommunication(String eventId, String id, String body) { return facade.editCommunication(eventId,id,body); }
    public Communication approveCommunication(String eventId, String id) { return facade.approveCommunication(eventId,id); }
    public String askFaq(String eventId, String question) { return facade.askFaq(eventId,question); }
    public void recordPostEvent(String eventId, PostEventData data) { facade.recordPostEvent(eventId,data); }
    public PostEventReport analyzePostEvent(String eventId) { return facade.analyzePostEvent(eventId); }
    public List<PostEventReport> viewPostEventReports(String eventId) { return facade.viewPostEventReports(eventId); }
    private final EventPlanningFacade facade;

    public void configureBudgetNeeds(String eventId, String currency, List<String> dietary, String catering) { facade.configureBudgetNeeds(eventId, currency, dietary, catering); }
    public void configureVenueNeeds(String eventId, boolean wheelchair, List<String> equipment) { facade.configureVenueNeeds(eventId, wheelchair, equipment); }
    public Budget generateBudget(String eventId, String priority) { return facade.generateBudget(eventId, priority); }
    public boolean hasBudgetDraft(String eventId) { return facade.hasBudgetDraft(eventId); }
    public Budget viewBudget(String eventId) { return facade.viewBudget(eventId); }
    public void editBudget(String eventId, BudgetItem replacement) { facade.editBudget(eventId, replacement); }
    public List<String> validateBudget(String eventId) { return facade.validateBudget(eventId); }
    public java.math.BigDecimal budgetTotal(String eventId) { return facade.budgetTotal(eventId); }
    public java.math.BigDecimal budgetRemaining(String eventId) { return facade.budgetRemaining(eventId); }
    public void approveBudget(String eventId) { facade.approveBudget(eventId); }
    public void discardBudget(String eventId) { facade.discardBudget(eventId); }
    public ToolResult researchVenues(String eventId, String query) { return facade.researchVenues(eventId, query); }
    public List<VenueOption> listVenues(String eventId) { return facade.listVenues(eventId); }
    public void addVenue(String eventId, VenueOption venue) { facade.addVenue(eventId, venue); }
    public List<VenueComparison> compareVenues(String eventId) { return facade.compareVenues(eventId); }

    public void configureMember(String eventId, User user) { facade.configureMember(eventId, user); }
    public List<User> listMembers(String eventId) { return facade.listMembers(eventId); }
    public void configureEventEnd(String eventId, LocalDateTime end) { facade.configureEventEnd(eventId, end); }
    public void configureSpeaker(String eventId, Speaker speaker) { facade.configureSpeaker(eventId, speaker); }
    public void configureResource(String eventId, String resource, List<TimeSlot> slots) { facade.configureResource(eventId, resource, slots); }
    public List<Task> generateTasks(String eventId) { return facade.generateTasks(eventId); }
    public boolean hasTaskDraft(String eventId) { return facade.hasTaskDraft(eventId); }
    public List<Task> listTasks(String eventId) { return facade.listTasks(eventId); }
    public List<String> taskWarnings(String eventId) { return facade.taskWarnings(eventId); }
    public void approveTasks(String eventId) { facade.approveTasks(eventId); }
    public void discardTasks(String eventId) { facade.discardTasks(eventId); }
    public void reviseTask(String eventId, String taskId, String userId, LocalDateTime start, LocalDateTime deadline) {
        facade.reviseTask(eventId, taskId, userId, start, deadline);
    }
    public void updateTaskStatus(String eventId, String taskId, Task.Status status) { facade.updateTaskStatus(eventId, taskId, status); }
    public Schedule buildSchedule(String eventId) { return facade.buildSchedule(eventId); }
    public boolean hasScheduleDraft(String eventId) { return facade.hasScheduleDraft(eventId); }
    public Schedule viewSchedule(String eventId) { return facade.viewSchedule(eventId); }
    public List<String> scheduleConflicts(String eventId) { return facade.scheduleConflicts(eventId); }
    public void reviseSession(String eventId, Session session) { facade.reviseSession(eventId, session); }
    public void approveSchedule(String eventId) { facade.approveSchedule(eventId); }
    public void discardSchedule(String eventId) { facade.discardSchedule(eventId); }

    public EventPlanningController(EventPlanningFacade facade) {
        this.facade = Objects.requireNonNull(facade, "Facade is required.");
    }

    public Event createEvent(String name, String type, String location,
            LocalDateTime dateTime, int attendance, double budget) {
        return facade.createEvent(name, type, location, dateTime, attendance, budget);
    }

    public List<Event> listEvents() {
        return facade.listEvents();
    }

    public Event viewEvent(String id) {
        return facade.viewEvent(id);
    }

    public boolean deleteEvent(String id) {
        return facade.deleteEvent(id);
    }

    public EventIdea addIdea(String eventId, String organizer, String idea) { return facade.addIdea(eventId, organizer, idea); }
    public List<EventIdea> listIdeas(String eventId) { return facade.listIdeas(eventId); }
    public void addConstraint(String eventId, String constraint) { facade.addConstraint(eventId, constraint); }
    public List<String> listConstraints(String eventId) { return facade.listConstraints(eventId); }
    public List<EventConcept> listConcepts(String eventId) { return facade.listConcepts(eventId); }
    public List<EventConcept> fuseIdeas(String eventId) { return facade.fuseIdeas(eventId); }
    public List<EventBlueprint> generateBlueprints(String eventId, String conceptId, String priority) {
        return facade.generateBlueprints(eventId, conceptId, priority);
    }
    public List<EventBlueprint> listBlueprints(String eventId) { return facade.listBlueprints(eventId); }
    public EventBlueprint viewBlueprint(String eventId, String blueprintId) { return facade.viewBlueprint(eventId, blueprintId); }
}
