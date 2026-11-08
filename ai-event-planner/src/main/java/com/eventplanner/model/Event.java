package com.eventplanner.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Event {
    private Budget budget;
    private String planningCurrency = "CAD";
    private List<String> dietaryRequirements = List.of();
    private String cateringNeeds = "Not specified";
    private boolean requiresWheelchairEntrance;
    private List<String> requiredEquipment = List.of();
    private final java.util.Map<String, VenueOption> venueOptions = new java.util.LinkedHashMap<>();
    private List<VenueComparison> venueComparisons = List.of();
    public Budget getBudget() { return budget; }
    public void storeBudget(Budget approved) { budget = java.util.Objects.requireNonNull(approved); }
    public String getPlanningCurrency() { return planningCurrency; }
    public List<String> getDietaryRequirements() { return dietaryRequirements; }
    public String getCateringNeeds() { return cateringNeeds; }
    public boolean requiresWheelchairEntrance() { return requiresWheelchairEntrance; }
    public List<String> getRequiredEquipment() { return requiredEquipment; }
    public void configureBudgetNeeds(String currency, List<String> dietary, String catering) {
        java.util.Currency.getInstance(currency);
        var checkedDietary = PlanningValues.texts(dietary, "Dietary requirements", true);
        var checkedCatering = PlanningValues.text(catering, "Catering needs");
        planningCurrency = currency; dietaryRequirements = checkedDietary; cateringNeeds = checkedCatering;
        venueComparisons = List.of();
    }
    public void configureVenueNeeds(boolean wheelchairEntrance, List<String> equipment) {
        requiredEquipment = PlanningValues.texts(equipment, "Required equipment", true);
        requiresWheelchairEntrance = wheelchairEntrance; venueComparisons = List.of();
    }
    public List<VenueOption> getVenueOptions() { return List.copyOf(venueOptions.values()); }
    public void addVenueOptions(List<VenueOption> options) {
        List.copyOf(options).forEach(option -> venueOptions.put(option.id(), option));
        venueComparisons = List.of();
    }
    public List<VenueComparison> getVenueComparisons() { return venueComparisons; }
    public void storeVenueComparisons(List<VenueComparison> comparisons) { venueComparisons = List.copyOf(comparisons); }
    private final PlanningTeam planningTeam = new PlanningTeam(java.util.UUID.randomUUID().toString(), "Event team");
    private LocalDateTime endDateTime;
    private final java.util.Map<String, Speaker> speakers = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, List<TimeSlot>> resourceAvailability = new java.util.LinkedHashMap<>();
    private List<Task> tasks = List.of();
    private Schedule schedule;

    public PlanningTeam getPlanningTeam() { return planningTeam; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime end) { new TimeSlot(dateTime, end); endDateTime = end; }
    public List<Speaker> getSpeakers() { return List.copyOf(speakers.values()); }
    public void putSpeaker(Speaker speaker) { speakers.put(speaker.id(), speaker); }
    public java.util.Map<String, List<TimeSlot>> getResourceAvailability() { return java.util.Map.copyOf(resourceAvailability); }
    public void putResourceAvailability(String resource, List<TimeSlot> slots) {
        resourceAvailability.put(PlanningValues.text(resource, "Resource ID").toLowerCase(java.util.Locale.ROOT), List.copyOf(slots));
    }
    public List<Task> getTasks() { return tasks; }
    public void storeTasks(List<Task> approved) { tasks = List.copyOf(approved); }
    public Schedule getSchedule() { return schedule; }
    public void storeSchedule(Schedule approved) { schedule = java.util.Objects.requireNonNull(approved); }

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
