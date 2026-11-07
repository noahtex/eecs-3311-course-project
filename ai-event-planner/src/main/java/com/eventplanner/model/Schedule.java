package com.eventplanner.model;

import java.util.List;

public record Schedule(String id, List<Session> sessions) {
    public Schedule {
        id = PlanningValues.text(id, "Schedule ID");
        sessions = List.copyOf(sessions);
        if (sessions.isEmpty()) throw new IllegalArgumentException("Schedule must contain sessions.");
        if (sessions.stream().map(Session::id).distinct().count() != sessions.size())
            throw new IllegalArgumentException("Session IDs must be unique.");
    }
}
