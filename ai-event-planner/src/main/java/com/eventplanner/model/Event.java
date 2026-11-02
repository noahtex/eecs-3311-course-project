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

    private List<String> ideas;

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
            ideas.add(idea);
        }
    }

    public List<String> getIdeas() {
        return List.copyOf(ideas);
    }
}
