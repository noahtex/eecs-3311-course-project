package com.eventplanner.service;

import com.eventplanner.model.Event;
import com.eventplanner.model.EventIdea;
import com.eventplanner.model.EventConcept;
import com.eventplanner.model.EventBlueprint;
import com.eventplanner.model.PlanningValues;
import java.util.List;

public class EventService {

    private volatile List<Event> events;

    public EventService() {
        this.events = new java.util.concurrent.CopyOnWriteArrayList<>();
    }

    /** Publish a fully restored graph only after validation; event selection can read without blocking AI work. */
    public void restoreEvents(List<Event> restored) {
        var ids=new java.util.HashSet<String>();
        for(Event event:restored)if(!ids.add(event.getId()))throw new IllegalArgumentException("Duplicate event ID.");
        events=new java.util.concurrent.CopyOnWriteArrayList<>(restored);
    }

    public void createEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null.");
        }

        for (Event existingEvent : events) {
            if (existingEvent.getId().equals(event.getId())) {
                throw new IllegalArgumentException(
                        "An event with this ID already exists.");
            }
        }

        events.add(event);
    }

    public Event getEvent(String eventId) {
        for (Event event : events) {
            if (event.getId().equals(eventId)) {
                return event;
            }
        }

        return null;
    }

    public List<Event> getAllEvents() {
        return List.copyOf(events);
    }

    public boolean deleteEvent(String eventId) {
        return events.removeIf(event -> event.getId().equals(eventId));
    }

    public Event requireEvent(String eventId) {
        Event event = getEvent(PlanningValues.text(eventId, "Event ID"));
        if (event == null) throw new IllegalArgumentException("No event found with ID: " + eventId);
        return event;
    }

    public EventIdea addIdea(String eventId, String organizer, String idea) {
        return requireEvent(eventId).addIdea(organizer, idea);
    }

    public List<EventIdea> listIdeas(String eventId) { return requireEvent(eventId).getOrganizerIdeas(); }
    public void addConstraint(String eventId, String constraint) { requireEvent(eventId).addConstraint(constraint); }
    public List<String> listConstraints(String eventId) { return requireEvent(eventId).getConstraints(); }
    public void storeConcepts(String eventId, List<EventConcept> concepts) { requireEvent(eventId).addConcepts(concepts); }
    public List<EventConcept> listConcepts(String eventId) { return requireEvent(eventId).getConcepts(); }
    public void storeBlueprints(String eventId, List<EventBlueprint> blueprints) { requireEvent(eventId).addBlueprints(blueprints); }
    public List<EventBlueprint> listBlueprints(String eventId) { return requireEvent(eventId).getBlueprints(); }

    public EventConcept getConcept(String eventId, String conceptId) {
        String id = PlanningValues.text(conceptId, "Concept ID");
        return listConcepts(eventId).stream().filter(concept -> concept.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No concept found for this event with ID: " + id));
    }

    public EventBlueprint getBlueprint(String eventId, String blueprintId) {
        String id = PlanningValues.text(blueprintId, "Blueprint ID");
        return listBlueprints(eventId).stream().filter(blueprint -> blueprint.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No blueprint found for this event with ID: " + id));
    }
}
