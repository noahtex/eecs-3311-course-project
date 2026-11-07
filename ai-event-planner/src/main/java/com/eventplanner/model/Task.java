package com.eventplanner.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/** Immutable task snapshot; null assignedUserId explicitly means unassigned. */
public record Task(String id, String title, String description, String assignedUserId,
        LocalDateTime workStart, LocalDateTime deadline, double effortHours,
        List<String> requiredSkills, Priority priority, Status status) {
    public enum Priority { LOW, MEDIUM, HIGH }
    public enum Status { TODO, IN_PROGRESS, DONE }
    public Task {
        id = PlanningValues.text(id, "Task ID");
        title = PlanningValues.text(title, "Task title");
        description = PlanningValues.text(description, "Task description");
        if (assignedUserId != null) assignedUserId = PlanningValues.text(assignedUserId, "Assigned user ID");
        new TimeSlot(workStart, deadline);
        if (!Double.isFinite(effortHours) || effortHours <= 0) throw new IllegalArgumentException("Effort must be positive and finite.");
        requiredSkills = PlanningValues.texts(requiredSkills, "Required skills", true);
        Objects.requireNonNull(priority, "Priority is required.");
        Objects.requireNonNull(status, "Status is required.");
    }
    public Task assign(String userId) { return new Task(id, title, description, userId, workStart, deadline, effortHours, requiredSkills, priority, status); }
    public Task revise(String userId, LocalDateTime start, LocalDateTime due) {
        return new Task(id, title, description, userId, start, due, effortHours, requiredSkills, priority, status);
    }
    public Task withStatus(Status value) { return new Task(id, title, description, assignedUserId, workStart, deadline, effortHours, requiredSkills, priority, value); }
}
