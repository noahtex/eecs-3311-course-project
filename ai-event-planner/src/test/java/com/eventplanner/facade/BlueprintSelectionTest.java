package com.eventplanner.facade;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import com.eventplanner.support.PlanningFixtures.FakeAIModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import static com.eventplanner.support.BlueprintFixtures.addAlternatives;
import static com.eventplanner.support.ExecutionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class BlueprintSelectionTest {
    private EventPlanningFacade facade(Event event, FakeAIModel model) {
        var events = new EventService(); events.createEvent(event);
        return new EventPlanningFacade(events, new EventPlanningAgent(model),
                q -> { throw new AssertionError("No Places"); }, q -> { throw new AssertionError("No catering"); });
    }
    private FakeAIModel model() {
        return new FakeAIModel(p -> p.contains("F03:") ? tasksJson("alex").toString() : scheduleJson().toString());
    }

    @Test void onlyEventOwnedBlueprintCanBeSelectedAndApprovalIsExplicit() {
        Event event = event(); var alternatives = addAlternatives(event); var model = model();
        var foreign = new Event("foreign", "Other", "Social", "Campus", START, 30, 500);
        var foreignBlueprint = addAlternatives(foreign).getFirst();
        try (var facade = facade(event, model)) {
            var controller = new EventPlanningController(facade);
            assertThrows(IllegalArgumentException.class, () -> controller.approveBlueprint(event.getId()));
            assertThrows(IllegalArgumentException.class, () -> controller.selectBlueprint(event.getId(), foreignBlueprint.id()));
            assertNull(event.getSelectedBlueprintId()); assertNull(event.getApprovedBlueprintId());
            controller.selectBlueprint(event.getId(), alternatives.getFirst().id());
            assertEquals(alternatives.getFirst().id(), event.getSelectedBlueprintId());
            assertNull(event.getApprovedBlueprintId());
            assertThrows(IllegalArgumentException.class, () -> controller.generateTasks(event.getId()));
            var error = assertThrows(IllegalArgumentException.class, () -> controller.buildSchedule(event.getId()));
            assertTrue(error.getMessage().contains("Select and approve")); assertTrue(model.prompts.isEmpty());
            controller.approveBlueprint(event.getId());
            assertEquals(alternatives.getFirst(), event.getApprovedBlueprint());
            assertEquals(alternatives, event.getBlueprints());
            assertThrows(IllegalArgumentException.class, () -> controller.selectBlueprint(event.getId(), "missing"));
            assertEquals(alternatives.getFirst().id(), event.getSelectedBlueprintId());
            assertEquals(alternatives.getFirst().id(), event.getApprovedBlueprintId());
        }
    }

    @Test void bothExecutionPromptsContainOnlyApprovedAlternativeEvenWithDifferentPendingSelection() throws Exception {
        Event event = event(); var alternatives = addAlternatives(event); var model = model();
        try (var facade = facade(event, model)) {
            facade.selectBlueprint(event.getId(), alternatives.getFirst().id()); facade.approveBlueprint(event.getId());
            facade.selectBlueprint(event.getId(), alternatives.getLast().id());
            facade.generateTasks(event.getId()); facade.buildSchedule(event.getId());
            assertEquals(2, model.prompts.size());
            for (String prompt : model.prompts) {
                String encoded = prompt.substring(prompt.lastIndexOf("\nContext: ")+"\nContext: ".length());
                var context = new ObjectMapper().readTree(encoded);
                assertEquals(1, context.path("blueprints").size());
                assertEquals(alternatives.getFirst().id(), context.path("blueprints").get(0).path("id").asText());
                assertTrue(encoded.contains("Quiet workshop checklist"));
                assertTrue(encoded.contains("Quiet workshop agenda"));
                assertTrue(encoded.contains("Quiet workshop resources"));
                assertFalse(encoded.contains("Loud concert"));
            }
            assertEquals(alternatives.getFirst().id(), event.snapshot().getApprovedBlueprintId());
            assertEquals(alternatives.getLast().id(), event.snapshot().getSelectedBlueprintId());
        }
    }

    @Test void noAlternativesUsesExplicitEventDetailsOnlyFallback() throws Exception {
        Event event = event(); var model = model();
        try (var facade = facade(event, model)) {
            assertEquals(2, facade.generateTasks(event.getId()).size());
            assertEquals(4, facade.buildSchedule(event.getId()).sessions().size());
            for (String prompt : model.prompts) {
                String encoded = prompt.substring(prompt.lastIndexOf("\nContext: ")+"\nContext: ".length());
                var context = new ObjectMapper().readTree(encoded);
                assertTrue(context.path("blueprints").isEmpty());
                assertTrue(context.path("blueprintStatus").asText().contains("event details, ideas and constraints only"));
            }
        }
    }

    @Test void switchingRetainsApprovedPlansAndDraftsAndFlagsBothForReview() {
        Event event = event(); var alternatives = addAlternatives(event); var model = model();
        try (var facade = facade(event, model)) {
            facade.selectBlueprint(event.getId(), alternatives.getFirst().id()); facade.approveBlueprint(event.getId());
            facade.generateTasks(event.getId()); facade.approveTasks(event.getId());
            facade.buildSchedule(event.getId()); facade.approveSchedule(event.getId());
            var approvedTasks = event.getTasks(); var approvedSchedule = event.getSchedule();
            var drafts = facade.generateTasks(event.getId()); var scheduleDraft = facade.buildSchedule(event.getId());
            facade.selectBlueprint(event.getId(), alternatives.getLast().id());
            assertTrue(event.getReviewWarnings().isEmpty());
            facade.approveBlueprint(event.getId());
            assertEquals(alternatives.getLast(), event.getApprovedBlueprint());
            assertSame(approvedTasks, event.getTasks()); assertSame(approvedSchedule, event.getSchedule());
            assertEquals(drafts, facade.listTasks(event.getId())); assertSame(scheduleDraft, facade.viewSchedule(event.getId()));
            assertTrue(facade.hasTaskDraft(event.getId())); assertTrue(facade.hasScheduleDraft(event.getId()));
            for (String component : List.of("tasks", "schedule")) {
                assertTrue(event.getReviewWarnings().get(component).stream().anyMatch(w -> w.contains("blueprint changed")));
            }
            assertEquals(alternatives, event.getBlueprints());
            facade.generateTasks(event.getId()); facade.buildSchedule(event.getId());
            for (String prompt : model.prompts.subList(model.prompts.size()-2, model.prompts.size())) {
                assertTrue(prompt.contains("Loud concert checklist")); assertFalse(prompt.contains("Quiet workshop checklist"));
            }
        }
    }

    @Test void firstApprovalFlagsExistingFallbackPlansWithoutReplacingThem() {
        Event event = event(); var tasks = List.of(task("existing", "alex")); var schedule = schedule();
        event.storeTasks(tasks); event.storeSchedule(schedule); var alternatives = addAlternatives(event);
        try (var facade = facade(event, model())) {
            facade.selectBlueprint(event.getId(), alternatives.getFirst().id()); facade.approveBlueprint(event.getId());
            assertEquals(tasks, event.getTasks()); assertSame(schedule, event.getSchedule());
            assertTrue(event.getReviewWarnings().keySet().containsAll(List.of("tasks", "schedule")));
        }
    }
}
