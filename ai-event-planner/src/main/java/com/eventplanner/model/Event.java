package com.eventplanner.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Event {
    private final java.util.Set<EventObserver> observers = new java.util.LinkedHashSet<>();
    private final java.util.Map<String,List<String>> reviewWarnings = new java.util.LinkedHashMap<>();
    private final List<ContingencyPlan> contingencies = new ArrayList<>();
    private final java.util.Map<String,Communication> communications = new java.util.LinkedHashMap<>();
    private PostEventData postEventData;
    private final List<PostEventReport> reports = new ArrayList<>();
    public void addObserver(EventObserver observer) { observers.add(java.util.Objects.requireNonNull(observer)); }
    public void flagReview(String component, List<String> issues) { reviewWarnings.put(component,List.copyOf(issues)); }
    public java.util.Map<String,List<String>> getReviewWarnings() { return java.util.Map.copyOf(reviewWarnings); }
    public List<ContingencyPlan> getContingencies() { return List.copyOf(contingencies); }
    public void addContingency(ContingencyPlan plan) { contingencies.add(plan); }
    public List<Communication> getCommunications() { return List.copyOf(communications.values()); }
    public void storeCommunication(Communication value) { communications.put(value.id(),value); }
    public PostEventData getPostEventData() { return postEventData; }
    public void storePostEventData(PostEventData data) { postEventData=java.util.Objects.requireNonNull(data); }
    public List<PostEventReport> getReports() { return List.copyOf(reports); }
    public void storeReport(PostEventReport report) { reports.add(report); }
    public java.util.Map<String,String> changeFields() {
        var values = new java.util.LinkedHashMap<String,String>();
        values.put("name",name); values.put("type",type); values.put("location",location);
        values.put("dateTime",dateTime.toString()); values.put("endDateTime",endDateTime==null?"":endDateTime.toString());
        values.put("attendance",Integer.toString(expectedAttendance)); values.put("budget",Double.toString(totalBudget));
        values.put("unavailableSpeaker", ""); return java.util.Map.copyOf(values);
    }
    /** Validate everything on an isolated candidate before atomic field assignment and notification. */
    public void applyChanges(java.util.Map<String,String> changes) {
        Event candidate = snapshot();
        try { candidate.assignChanges(changes); }
        catch(java.time.DateTimeException ex) { throw new IllegalArgumentException("Changed dates must be valid ISO local date/times."); }
        name=candidate.name; type=candidate.type; location=candidate.location; dateTime=candidate.dateTime;
        endDateTime=candidate.endDateTime; expectedAttendance=candidate.expectedAttendance; totalBudget=candidate.totalBudget;
        speakers.clear(); candidate.speakers.forEach(speakers::put);
        if(!communications.isEmpty()) {
            communications.replaceAll((key,value)->value.edit(value.text()));
            flagReview("communications",List.of("Event changed. Communications reverted to drafts; regenerate and verify facts."));
        }
        observers.forEach(observer -> observer.onEventChanged(this,java.util.Set.copyOf(changes.keySet())));
    }
    private void assignChanges(java.util.Map<String,String> changes) {
        for(var entry: changes.entrySet()) {
            String value=entry.getValue();
            switch(entry.getKey()) {
                case "name" -> name=PlanningValues.text(value,"Name");
                case "type" -> type=PlanningValues.text(value,"Type");
                case "location" -> location=PlanningValues.text(value,"Location");
                case "dateTime" -> dateTime=LocalDateTime.parse(value);
                case "endDateTime" -> endDateTime=LocalDateTime.parse(value);
                case "attendance" -> { expectedAttendance=Integer.parseInt(value); if(expectedAttendance<0) throw new IllegalArgumentException("Attendance cannot be negative."); }
                case "budget" -> { totalBudget=Double.parseDouble(value); if(!Double.isFinite(totalBudget)||totalBudget<0) throw new IllegalArgumentException("Invalid budget."); }
                case "unavailableSpeaker" -> { if(speakers.remove(value)==null) throw new IllegalArgumentException("Unknown speaker ID."); }
                default -> throw new IllegalArgumentException("Unsupported changed field: "+entry.getKey());
            }
        }
        if(endDateTime!=null) new TimeSlot(dateTime,endDateTime);
    }
    /** Immutable value records can be shared; all mutable aggregate containers and users are copied. */
    public Event snapshot() {
        Event copy=new Event(id,name,type,location,dateTime,expectedAttendance,totalBudget);
        copy.endDateTime=endDateTime; copy.budget=budget; copy.planningCurrency=planningCurrency;
        copy.dietaryRequirements=dietaryRequirements; copy.cateringNeeds=cateringNeeds;
        copy.requiresWheelchairEntrance=requiresWheelchairEntrance; copy.requiredEquipment=requiredEquipment;
        copy.venueOptions.putAll(venueOptions); copy.venueComparisons=venueComparisons;
        copy.speakers.putAll(speakers); copy.resourceAvailability.putAll(resourceAvailability);
        copy.tasks=tasks; copy.schedule=schedule; copy.ideas.addAll(ideas); copy.constraints.addAll(constraints);
        copy.concepts.addAll(concepts); copy.blueprints.addAll(blueprints);
        for(User user: planningTeam.getMembers()) {
            User member=new User(user.getId(),user.getName(),user.getEmail(),user.getRole());
            member.configurePlanning(user.getSkills(),user.getPreferences(),user.getAvailability(),user.getExistingWorkloadHours(),user.getCapacityHours());
            copy.planningTeam.addMember(member);
        }
        return copy;
    }
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
    private final PlanningTeam planningTeam;
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
        this(id,name,type,location,dateTime,expectedAttendance,totalBudget,new PlanningTeam(java.util.UUID.randomUUID().toString(),"Event team"));
    }

    /** Restore an existing team identity; observers are deliberately not part of stored data. */
    public Event(String id,String name,String type,String location,LocalDateTime dateTime,int expectedAttendance,double totalBudget,PlanningTeam team) {
        this.planningTeam = java.util.Objects.requireNonNull(team);

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
    /** Data restoration preserves idea IDs and submission times instead of generating new ones. */
    public void restoreOrganizerIdeas(List<EventIdea> restored) { ideas.clear();ideas.addAll(List.copyOf(restored)); }
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
