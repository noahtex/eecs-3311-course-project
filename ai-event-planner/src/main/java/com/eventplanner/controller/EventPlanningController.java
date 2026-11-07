package com.eventplanner.controller;

import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.Event;
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
}
