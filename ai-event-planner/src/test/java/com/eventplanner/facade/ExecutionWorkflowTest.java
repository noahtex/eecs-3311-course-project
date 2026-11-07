package com.eventplanner.facade;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import com.eventplanner.support.PlanningFixtures.FakeAIModel;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.ExecutionFixtures.*;

class ExecutionWorkflowTest {
    private EventPlanningController controller(EventService service, String assignee) {
        return new EventPlanningController(new EventPlanningFacade(service, new EventPlanningAgent(prompt ->
                prompt.contains("F03:") ? tasksJson(assignee).toString() : scheduleJson().toString())));
    }
    @Test void draftsRequireApprovalAndEditsPreserveApprovedTasksUntilReapproved() {
        var event = event();
        var service = new EventService(); service.createEvent(event);
        var controller = controller(service, "alex");
        var proposed = controller.generateTasks(event.getId());
        assertTrue(event.getTasks().isEmpty());
        assertTrue(controller.hasTaskDraft(event.getId()));
        controller.approveTasks(event.getId());
        assertEquals(proposed, event.getTasks());
        assertFalse(controller.hasTaskDraft(event.getId()));
        var task = proposed.get(0);
        controller.reviseTask(event.getId(), task.id(), "invented", task.workStart(), task.deadline());
        assertThrows(IllegalArgumentException.class, () -> controller.approveTasks(event.getId()));
        assertEquals(proposed, event.getTasks());
        controller.reviseTask(event.getId(), task.id(), null, task.workStart(), task.deadline());
        assertTrue(controller.taskWarnings(event.getId()).stream().anyMatch(s -> s.contains("unassigned")));
        controller.approveTasks(event.getId());
        assertNull(event.getTasks().get(0).assignedUserId());
        controller.updateTaskStatus(event.getId(), task.id(), Task.Status.IN_PROGRESS);
        assertEquals(Task.Status.TODO, event.getTasks().get(0).status());
        controller.approveTasks(event.getId());
        assertEquals(Task.Status.IN_PROGRESS, event.getTasks().get(0).status());
        assertThrows(IllegalArgumentException.class, () -> controller.reviseTask(event.getId(), "missing", null, WORK, WORK.plusHours(3)));
    }
    @Test void unsuitableOrInventedAiAssigneesBecomeUnassignedWithWarnings() {
        for (String assignee : List.of("invented", "busy", "unavailable")) {
            var event = event();
            event.getPlanningTeam().addMember(member("busy", 10, 10, List.of(new TimeSlot(WORK.minusDays(1), START))));
            event.getPlanningTeam().addMember(member("unavailable", 0, 10, List.of(new TimeSlot(START, START.plusHours(1)))));
            var service = new EventService(); service.createEvent(event);
            var controller = controller(service, assignee);
            assertTrue(controller.generateTasks(event.getId()).stream().allMatch(task -> task.assignedUserId() == null));
            assertFalse(controller.taskWarnings(event.getId()).isEmpty());
            controller.approveTasks(event.getId());
            assertEquals(2, event.getTasks().size());
        }
    }
    @Test void scheduleRevisionIsValidatedAndDoesNotReplaceApprovedAgendaUntilApproval() {
        var event = event();
        var service = new EventService(); service.createEvent(event);
        var controller = controller(service, null);
        Schedule draft = controller.buildSchedule(event.getId());
        assertNull(event.getSchedule());
        assertTrue(controller.scheduleConflicts(event.getId()).isEmpty());
        controller.approveSchedule(event.getId());
        assertSame(draft, event.getSchedule());
        Session original = draft.sessions().get(1);
        Session invalid = new Session(original.id(), original.title(), original.kind(),
                new TimeSlot(START, START.plusHours(2)), original.speakerId(), original.space(), original.resources());
        controller.reviseSession(event.getId(), invalid);
        assertFalse(controller.scheduleConflicts(event.getId()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> controller.approveSchedule(event.getId()));
        assertSame(draft, event.getSchedule());
        controller.reviseSession(event.getId(), original);
        controller.approveSchedule(event.getId());
        assertEquals(draft, event.getSchedule());
    }
    @Test void approvalsRecheckChangedAvailabilityAndCrossEventWorkload() {
        var event = event();
        var service = new EventService(); service.createEvent(event);
        var controller = controller(service, "alex");
        controller.generateTasks(event.getId());
        var other = new Event("other", "Other", "T", "Room", START, 10, 50);
        other.storeTasks(List.of(new Task("other-task", "Other work", "Already approved elsewhere", "alex", WORK,
                WORK.plusHours(9), 8, List.of(), Task.Priority.HIGH, Task.Status.TODO)));
        service.createEvent(other);
        assertThrows(IllegalArgumentException.class, () -> controller.approveTasks(event.getId()));
        assertTrue(event.getTasks().isEmpty());
        controller.discardTasks(event.getId());
        assertFalse(controller.hasTaskDraft(event.getId()));
        controller.buildSchedule(event.getId());
        controller.configureSpeaker(event.getId(), new Speaker("speaker", "Speaker", List.of()));
        assertThrows(IllegalArgumentException.class, () -> controller.approveSchedule(event.getId()));
        controller.discardSchedule(event.getId());
        assertFalse(controller.hasScheduleDraft(event.getId()));
    }
    @Test void failedGenerationDoesNotReplaceExistingApprovedPlansOrDraftsAndRejectsUnknownEventsBeforeAi() {
        var event = event();
        event.storeTasks(List.of(task("approved", null)));
        event.storeSchedule(schedule());
        var service = new EventService(); service.createEvent(event);
        var model = new FakeAIModel(prompt -> "{}");
        var controller = new EventPlanningController(new EventPlanningFacade(service, new EventPlanningAgent(model)));
        assertThrows(IllegalArgumentException.class, () -> controller.generateTasks("missing"));
        assertThrows(IllegalArgumentException.class, () -> controller.buildSchedule("missing"));
        assertTrue(model.prompts.isEmpty());
        assertThrows(PlanningException.class, () -> controller.generateTasks(event.getId()));
        assertThrows(PlanningException.class, () -> controller.buildSchedule(event.getId()));
        assertEquals("approved", event.getTasks().get(0).id());
        assertEquals(schedule(), event.getSchedule());
        assertFalse(controller.hasTaskDraft(event.getId()));
        assertFalse(controller.hasScheduleDraft(event.getId()));
    }
}
