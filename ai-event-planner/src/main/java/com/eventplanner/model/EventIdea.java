package com.eventplanner.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record EventIdea(String id, String organizer, String text, LocalDateTime submittedAt) {
    public EventIdea {
        id = PlanningValues.text(id, "Idea ID");
        organizer = PlanningValues.text(organizer, "Organizer");
        text = PlanningValues.text(text, "Idea");
        Objects.requireNonNull(submittedAt, "Submission time is required.");
    }
}
