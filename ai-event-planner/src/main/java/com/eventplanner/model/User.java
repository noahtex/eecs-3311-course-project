package com.eventplanner.model;

public class User {
    private java.util.List<String> skills = java.util.List.of();
    private java.util.List<String> preferences = java.util.List.of();
    private java.util.List<TimeSlot> availability = java.util.List.of();
    private double existingWorkloadHours;
    private double capacityHours;

    public void configurePlanning(java.util.List<String> skills, java.util.List<String> preferences,
            java.util.List<TimeSlot> availability, double existingWorkloadHours, double capacityHours) {
        var checkedSkills = PlanningValues.texts(skills, "Skills", true);
        var checkedPreferences = PlanningValues.texts(preferences, "Preferences", true);
        var checkedAvailability = java.util.List.copyOf(availability);
        if (!Double.isFinite(existingWorkloadHours) || existingWorkloadHours < 0
                || !Double.isFinite(capacityHours) || capacityHours < 0)
            throw new IllegalArgumentException("Workload and capacity must be finite non-negative hours.");
        this.skills = checkedSkills;
        this.preferences = checkedPreferences;
        this.availability = checkedAvailability;
        this.existingWorkloadHours = existingWorkloadHours;
        this.capacityHours = capacityHours;
    }
    public java.util.List<String> getSkills() { return skills; }
    public java.util.List<String> getPreferences() { return preferences; }
    public java.util.List<TimeSlot> getAvailability() { return availability; }
    public double getExistingWorkloadHours() { return existingWorkloadHours; }
    public double getCapacityHours() { return capacityHours; }
    
    private String id;
    private String name;
    private String email;
    private String role;

    public User(String id, String name, String email, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }
}
