package com.eventplanner.model;

import java.util.List;
import java.util.Objects;

public record Session(String id, String title, Kind kind, TimeSlot slot, String speakerId,
        String space, List<String> resources) {
    public enum Kind { ACTIVITY, BREAK, SETUP, TEARDOWN }
    public Session {
        id = PlanningValues.text(id, "Session ID");
        title = PlanningValues.text(title, "Session title");
        Objects.requireNonNull(kind, "Session kind is required.");
        Objects.requireNonNull(slot, "Session slot is required.");
        if (speakerId != null) speakerId = PlanningValues.text(speakerId, "Speaker ID");
        space = PlanningValues.text(space, "Session space");
        resources = PlanningValues.texts(resources, "Resources", true);
    }
}
