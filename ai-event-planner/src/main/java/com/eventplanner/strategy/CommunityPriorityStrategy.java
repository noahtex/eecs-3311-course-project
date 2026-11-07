package com.eventplanner.strategy;

import com.eventplanner.model.Event;

public final class CommunityPriorityStrategy implements PlanningStrategy {
    public String name() { return "community"; }
    public String buildGuidance(Event event) {
        return "Prioritize inclusive participation, accessibility, small-group interaction and club relationships for "
                + event.getExpectedAttendance() + " attendees. Explain trade-offs against spending and reach.";
    }
}
