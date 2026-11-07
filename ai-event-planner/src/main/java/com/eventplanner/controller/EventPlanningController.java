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
    private final EventPlanningFacade facade;

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
