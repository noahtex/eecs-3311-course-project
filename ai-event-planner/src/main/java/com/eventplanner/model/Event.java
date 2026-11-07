package com.eventplanner.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Event {

    private String id;
    private String name;
    private String type;
    private String location;
    private LocalDateTime dateTime;
    private int expectedAttendance;
    private double totalBudget;

    private final List<EventIdea> ideas;
    private final List<String> constraints = new ArrayList<>();
    private final List<EventConcept> concepts = new ArrayList<>();
    private final List<EventBlueprint> blueprints = new ArrayList<>();

    public Event(String id, String name, String type,
            String location, LocalDateTime dateTime,
            int expectedAttendance, double totalBudget) {

        this.id = id;
        this.name = name;
        this.type = type;
        this.location = location;
        this.dateTime = dateTime;
        this.expectedAttendance = expectedAttendance;
        this.totalBudget = totalBudget;

        this.ideas = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public int getExpectedAttendance() {
        return expectedAttendance;
    }

    public double getTotalBudget() {
        return totalBudget;
    }

    public void addIdea(String idea) {
        if (idea != null && !idea.isBlank()) {
            addIdea("Unspecified organizer", idea);
        }
    }

    public List<String> getIdeas() {
        return ideas.stream().map(EventIdea::text).toList();
    }

    public EventIdea addIdea(String organizer, String text) {
        EventIdea idea = new EventIdea(java.util.UUID.randomUUID().toString(), organizer, text, LocalDateTime.now());
        ideas.add(idea);
        return idea;
    }

    public List<EventIdea> getOrganizerIdeas() { return List.copyOf(ideas); }
    public void addConstraint(String constraint) { constraints.add(PlanningValues.text(constraint, "Constraint")); }
    public List<String> getConstraints() { return List.copyOf(constraints); }
    public List<EventConcept> getConcepts() { return List.copyOf(concepts); }
    public List<EventBlueprint> getBlueprints() { return List.copyOf(blueprints); }

    public void addConcepts(List<EventConcept> generated) {
        concepts.addAll(List.copyOf(generated));
    }

    public void addBlueprints(List<EventBlueprint> generated) {
        List<EventBlueprint> snapshot = List.copyOf(generated);
        if (snapshot.stream().anyMatch(blueprint -> concepts.stream()
                .noneMatch(concept -> concept.id().equals(blueprint.conceptId())))) {
            throw new IllegalArgumentException("Blueprint must reference a concept belonging to this event.");
        }
        blueprints.addAll(snapshot);
    }
}
