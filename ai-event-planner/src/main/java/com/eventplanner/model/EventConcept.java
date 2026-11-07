package com.eventplanner.model;

import java.util.List;

/** F01 proposal. incorporatedIdeas contains source idea IDs for traceability. */
public record EventConcept(String id, String title, String summary, List<String> themes,
        List<String> incorporatedIdeas, List<String> conflicts, List<String> compromises,
        List<String> uncertainties) {
    public EventConcept {
        id = PlanningValues.text(id, "Concept ID");
        title = PlanningValues.text(title, "Concept title");
        summary = PlanningValues.text(summary, "Concept summary");
        themes = PlanningValues.texts(themes, "Themes", false);
        incorporatedIdeas = PlanningValues.texts(incorporatedIdeas, "Incorporated idea IDs", false);
        conflicts = PlanningValues.texts(conflicts, "Conflicts", true);
        compromises = PlanningValues.texts(compromises, "Compromises", conflicts.isEmpty());
        uncertainties = PlanningValues.texts(uncertainties, "Uncertainties", true);
    }
}
