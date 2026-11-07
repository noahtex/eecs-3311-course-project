package com.eventplanner.controller;

import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.Event;
import com.eventplanner.model.EventIdea;
import com.eventplanner.model.EventConcept;
import com.eventplanner.model.EventBlueprint;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** UI-independent controller shared by application views. */
public class EventPlanningController {
    private final EventPlanningFacade facade;

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
