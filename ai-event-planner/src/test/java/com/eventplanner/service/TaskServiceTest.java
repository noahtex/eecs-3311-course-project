package com.eventplanner.service;

import com.eventplanner.model.*;
import com.eventplanner.support.ExecutionFixtures;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.ExecutionFixtures.*;

class TaskServiceTest {
    private final TaskService service = new TaskService(Clock.fixed(Instant.parse("2099-01-01T00:00:00Z"), ZoneOffset.UTC));
    @Test void acceptsSkilledAvailableMemberAndStoresImmutableApprovedPlan() {
        Event event = event();
        Task task = task("task", "alex");
        assertTrue(service.validateAssignments(event, List.of(task)).isEmpty());
        service.saveAssignments(event, List.of(task), List.of());
        assertEquals(List.of(task), event.getTasks());
        assertThrows(UnsupportedOperationException.class, () -> event.getTasks().clear());
    }
    @Test void unassignedTasksAreAllowedButExplicitlyWarned() {
        Event event = event();
        Task task = task("task", null);
        assertTrue(service.validateAssignments(event, List.of(task)).isEmpty());
        assertTrue(service.warnings(event, List.of(task), List.of()).get(0).contains("unassigned"));
        service.saveAssignments(event, List.of(task), List.of());
        assertNull(event.getTasks().get(0).assignedUserId());
    }
    @Test void detectsCombinedAndExternalWorkloadWithoutCountingDoneTasks() {
        Event event = event();
        event.getPlanningTeam().addMember(member("busy", 2, 5, List.of(new TimeSlot(WORK.minusDays(1), START))));
        var first = task("one", "busy");
        var second = task("two", "busy");
        assertTrue(service.validateAssignments(event, List.of(first, second)).stream().anyMatch(s -> s.contains("overloaded")));
        assertFalse(service.validateAssignments(event, List.of(first), List.of(second)).isEmpty());
        assertTrue(service.validateAssignments(event, List.of(first), List.of(second.withStatus(Task.Status.DONE))).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.saveAssignments(event, List.of(first, second), List.of()));
        assertTrue(event.getTasks().isEmpty());
    }
    @Test void flagsUnavailableUnknownAvailabilityUnknownMembersAndMissingSkills() {
        Event event = event();
        event.getPlanningTeam().addMember(member("unavailable", 0, 10, List.of(new TimeSlot(START, START.plusHours(1)))));
        event.getPlanningTeam().addMember(member("unknown", 0, 10, List.of()));
        var unskilled = new User("unskilled", "Sam", "", "Planner");
        unskilled.configurePlanning(List.of("cooking"), List.of(), List.of(new TimeSlot(WORK.minusDays(1), START)), 0, 10);
        event.getPlanningTeam().addMember(unskilled);
        for (String id : List.of("unavailable", "unknown", "invented", "unskilled")) {
            assertFalse(service.validateAssignments(event, List.of(task("t", id))).isEmpty(), id);
            assertThrows(IllegalArgumentException.class, () -> service.saveAssignments(event, List.of(task("t", id)), List.of()));
        }
    }
    @Test void validatesDeadlinesEffortAndDuplicateIds() {
        Event event = event();
        var task = task("t", null);
        assertThrows(IllegalArgumentException.class, () -> task.revise(null, WORK, WORK));
        assertFalse(service.validateAssignments(event, List.of(task.revise(null, START, START.plusHours(3)))).isEmpty());
        var past = task.revise(null, WORK.minusYears(1), WORK.minusYears(1).plusHours(3));
        assertTrue(service.validateAssignments(event, List.of(past)).stream().anyMatch(s -> s.contains("deadline has passed")));
        var shortWindow = task.revise(null, WORK, WORK.plusMinutes(30));
        assertTrue(service.validateAssignments(event, List.of(shortWindow)).stream().anyMatch(s -> s.contains("effort")));
        assertTrue(service.validateAssignments(event, List.of(task, task)).stream().anyMatch(s -> s.contains("duplicate")));
    }
}
