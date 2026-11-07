package com.eventplanner.facade;

import com.eventplanner.model.Event;
import com.eventplanner.service.EventService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Application operations and validation over the existing event service. */
public class EventPlanningFacade {
    private final EventService eventService;

    public EventPlanningFacade(EventService eventService) {
        this.eventService = Objects.requireNonNull(eventService, "Event service is required.");
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

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required.");
        }
        return value.trim();
    }
}
