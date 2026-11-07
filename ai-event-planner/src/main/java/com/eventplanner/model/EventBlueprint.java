package com.eventplanner.model;

import java.util.List;

/** F02 proposal. Numeric costs and capacity are planning assumptions, never vendor facts. */
public record EventBlueprint(String id, String conceptId, String priority, String title,
        String summary, List<String> agenda, List<String> venueRequirements,
        List<String> cateringAssumptions, List<String> resources, List<String> checklist,
        List<String> assumptions, List<String> feasibilityWarnings,
        double estimatedBudget, int plannedAttendance, int requiredCapacity) {
    public EventBlueprint {
        id = PlanningValues.text(id, "Blueprint ID");
        conceptId = PlanningValues.text(conceptId, "Concept ID");
        priority = PlanningValues.text(priority, "Priority");
        title = PlanningValues.text(title, "Blueprint title");
        summary = PlanningValues.text(summary, "Blueprint summary");
        agenda = PlanningValues.texts(agenda, "Agenda", false);
        venueRequirements = PlanningValues.texts(venueRequirements, "Venue requirements", false);
        cateringAssumptions = PlanningValues.texts(cateringAssumptions, "Catering assumptions", false);
        resources = PlanningValues.texts(resources, "Resources", false);
        checklist = PlanningValues.texts(checklist, "Checklist", false);
        assumptions = PlanningValues.texts(assumptions, "Assumptions", false);
        feasibilityWarnings = PlanningValues.texts(feasibilityWarnings, "Feasibility warnings", true);
        if (!Double.isFinite(estimatedBudget) || estimatedBudget < 0) {
            throw new IllegalArgumentException("Estimated budget must be finite and non-negative.");
        }
        if (plannedAttendance <= 0 || requiredCapacity <= 0) {
            throw new IllegalArgumentException("Planned attendance and required capacity must be positive integers.");
        }
    }
}
