package com.eventplanner.service;

import com.eventplanner.model.Event;
import java.util.ArrayList;
import java.util.List;

public class EventService {

    private List<Event> events;

    public EventService() {
        this.events = new ArrayList<>();
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
}
