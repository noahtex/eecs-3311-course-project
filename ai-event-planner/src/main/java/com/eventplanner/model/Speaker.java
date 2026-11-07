package com.eventplanner.model;

import java.util.List;

public record Speaker(String id, String name, List<TimeSlot> availability) {
    public Speaker {
        id = PlanningValues.text(id, "Speaker ID");
        name = PlanningValues.text(name, "Speaker name");
        availability = List.copyOf(availability);
    }
    public boolean checkAvailability(TimeSlot slot) { return availability.stream().anyMatch(value -> value.contains(slot)); }
}
