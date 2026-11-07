package com.eventplanner.strategy;

import com.eventplanner.model.Event;

public interface PlanningStrategy {
    String name();
    String buildGuidance(Event event);

    static PlanningStrategy fromName(String priority) {
        return switch (com.eventplanner.model.PlanningValues.text(priority, "Priority").toLowerCase(java.util.Locale.ROOT)) {
            case "budget" -> new BudgetPriorityStrategy();
            case "community" -> new CommunityPriorityStrategy();
            case "attendance" -> new AttendancePriorityStrategy();
            default -> throw new IllegalArgumentException("Choose budget, community or attendance priority.");
        };
    }
}
