package com.eventplanner.facade;

import com.eventplanner.model.Event;
import com.eventplanner.model.EventIdea;
import com.eventplanner.model.EventConcept;
import com.eventplanner.model.EventBlueprint;
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
public class EventPlanningFacade {
    private final EventService eventService;
    private final EventPlanningAgent agent;

    public EventPlanningFacade(EventService eventService) {
        this(eventService, new EventPlanningAgent(new GeminiAdapter(new GeminiAPIClient())));
    }

    public EventPlanningFacade(EventService eventService, EventPlanningAgent agent) {
        this.eventService = Objects.requireNonNull(eventService, "Event service is required.");
        this.agent = Objects.requireNonNull(agent, "Planning agent is required.");
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
        return eventService.deleteEvent(required(id, "Event ID"));
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
