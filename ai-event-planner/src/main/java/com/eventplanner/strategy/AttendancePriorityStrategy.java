package com.eventplanner.strategy;

import com.eventplanner.model.Event;

public final class AttendancePriorityStrategy implements PlanningStrategy {
    public String name() { return "attendance"; }
    public String buildGuidance(Event event) {
        return "Prioritize reaching the attendance target " + event.getExpectedAttendance()
                + ", broad appeal, accessible timing and sufficient capacity. Explain outreach needs and budget trade-offs; do not promise turnout.";
    }
}
