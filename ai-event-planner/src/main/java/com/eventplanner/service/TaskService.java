package com.eventplanner.service;

import com.eventplanner.model.*;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/** Deterministic assignment validation. Capacity is total active hours for this planning horizon. */
public final class TaskService implements EventObserver {
    @Override public void onEventChanged(Event event, Set<String> fields) { if(!event.getTasks().isEmpty()) { var issues=new ArrayList<String>(); issues.add("Tasks require review after changes: "+fields); issues.addAll(warnings(event,event.getTasks(),List.of())); event.flagReview("tasks",issues); } }
    private final Clock clock;
    public TaskService() { this(Clock.systemDefaultZone()); }
    public TaskService(Clock clock) { this.clock = Objects.requireNonNull(clock); }

    public List<String> validateAssignments(Event event, List<Task> plan) { return validateAssignments(event, plan, List.of()); }
    public List<String> validateAssignments(Event event, List<Task> plan, List<Task> otherEvents) {
        List<String> issues = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (Task task : plan) {
            String prefix = "Task " + task.id() + " (" + task.title() + "): ";
            if (!ids.add(task.id())) issues.add(prefix + "duplicate task ID.");
            if (task.status() != Task.Status.DONE && task.deadline().isBefore(LocalDateTime.now(clock))) issues.add(prefix + "deadline has passed.");
            if (task.deadline().isAfter(event.getDateTime())) issues.add(prefix + "deadline is after event start.");
            if (task.effortHours() > Duration.between(task.workStart(), task.deadline()).toMinutes() / 60.0)
                issues.add(prefix + "effort exceeds the work window.");
            if (task.assignedUserId() == null) continue;
            User member = event.getPlanningTeam().getMembers().stream()
                    .filter(user -> user.getId().equals(task.assignedUserId())).findFirst().orElse(null);
            if (member == null) { issues.add(prefix + "unknown team member."); continue; }
            if (!task.requiredSkills().stream().allMatch(skill -> member.getSkills().stream().anyMatch(skill::equalsIgnoreCase)))
                issues.add(prefix + "member lacks required skills.");
            TimeSlot work = new TimeSlot(task.workStart(), task.deadline());
            if (member.getAvailability().isEmpty()) issues.add(prefix + "member availability is unknown.");
            else if (member.getAvailability().stream().noneMatch(slot -> slot.contains(work)))
                issues.add(prefix + "member is unavailable for the work window.");
            double activeHours = java.util.stream.Stream.concat(plan.stream(), otherEvents.stream())
                    .filter(value -> member.getId().equals(value.assignedUserId()) && value.status() != Task.Status.DONE)
                    .mapToDouble(Task::effortHours).sum();
            if (member.getExistingWorkloadHours() + activeHours > member.getCapacityHours())
                issues.add(prefix + "member is overloaded (existing plus active task hours exceed capacity).");
        }
        return List.copyOf(issues);
    }

    public List<String> warnings(Event event, List<Task> plan, List<Task> otherEvents) {
        List<String> warnings = new ArrayList<>(validateAssignments(event, plan, otherEvents));
        plan.stream().filter(task -> task.assignedUserId() == null && task.status() != Task.Status.DONE)
                .forEach(task -> warnings.add("Task " + task.id() + ": unassigned; planner action required."));
        return List.copyOf(warnings);
    }

    public void saveAssignments(Event event, List<Task> approved, List<Task> otherEvents) {
        var issues = validateAssignments(event, approved, otherEvents);
        if (!issues.isEmpty()) throw new IllegalArgumentException("Task plan cannot be approved: " + String.join(" ", issues));
        event.storeTasks(approved);
    }
}
