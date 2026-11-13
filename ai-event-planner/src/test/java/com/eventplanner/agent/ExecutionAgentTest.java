package com.eventplanner.agent;

import com.eventplanner.ai.PlanningException;
import com.eventplanner.model.*;
import com.eventplanner.support.PlanningFixtures.FakeAIModel;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.ExecutionFixtures.*;

class ExecutionAgentTest {
    private final java.util.List<EventPlanningAgent> agents = new java.util.ArrayList<>();
    private EventPlanningAgent agent(com.eventplanner.ai.AIModel model) {
        var agent = new EventPlanningAgent(model);
        agents.add(agent);
        return agent;
    }
    @AfterEach void closeAgents() {
        // Agents own their models; do not close those models separately.
        agents.forEach(EventPlanningAgent::close);
    }

    @Test void generatesStructuredTasksAndIncludesCapabilitiesPreferencesAndWorkloadWithoutEmail() {
        Event event = event();
        var model = new FakeAIModel(prompt -> tasksJson("alex").toString());
        var tasks = agent(model).proposeTaskAssignments(event, List.of(task("other-event", "alex")));
        assertEquals(2, tasks.size());
        assertNotEquals(tasks.get(0).id(), tasks.get(1).id());
        assertEquals(Task.Status.TODO, tasks.get(0).status());
        String prompt = model.prompts.get(0);
        for (String field : List.of("Craft workshop", "logistics", "craft", "availability", "existingWorkloadHours", "capacityHours", "activeTasks"))
            assertTrue(prompt.contains(field), field);
        assertFalse(prompt.contains("not-sent@example.invalid"));
        assertTrue(event.getTasks().isEmpty());
    }
    @Test void generatesCompleteStructuredAgendaWithoutSaving() {
        var event = event();
        var model = new FakeAIModel(prompt -> scheduleJson().toString());
        var agenda = agent(model).proposeSchedule(event);
        assertEquals(4, agenda.sessions().size());
        assertEquals(4, agenda.sessions().stream().map(Session::kind).distinct().count());
        assertTrue(model.prompts.get(0).contains("resourceAvailability"));
        assertTrue(model.prompts.get(0).contains("speaker"));
        assertNull(event.getSchedule());
    }
    @Test void malformedTaskOutputRetriesOnceAndRecovers() {
        var calls = new AtomicInteger();
        var model = new FakeAIModel(prompt -> calls.getAndIncrement() == 0 ? "not JSON" : tasksJson(null).toString());
        assertNull(agent(model).proposeTaskAssignments(event(), List.of()).get(0).assignedUserId());
        assertEquals(2, model.prompts.size());
        assertTrue(model.prompts.get(1).contains("RETRY:"));
    }
    @Test void rejectsMissingFieldsWrongTypesInvalidDatesAndInvalidEnumsAfterTwoAttempts() {
        for (String field : List.of("title", "description", "assignedUserId", "workStart", "deadline", "effortHours", "requiredSkills", "priority")) {
            ObjectNode output = tasksJson(null);
            ((ObjectNode) output.path("tasks").get(0)).remove(field);
            assertInvalidTasks(output.toString());
        }
        for (String invalid : List.of("null", "{}", "{\"tasks\":[]}", "{\"tasks\":[{},{}]}")) assertInvalidTasks(invalid);
        for (String field : List.of("workStart", "deadline", "priority", "effortHours")) {
            var output = tasksJson(null);
            ((ObjectNode) output.path("tasks").get(0)).put(field, "invalid");
            assertInvalidTasks(output.toString());
        }
        for (String field : List.of("title", "kind", "start", "end", "speakerId", "space", "resources")) {
            var output = scheduleJson();
            ((ObjectNode) output.path("sessions").get(0)).remove(field);
            var model = new FakeAIModel(prompt -> output.toString());
            assertThrows(PlanningException.class, () -> agent(model).proposeSchedule(event()));
            assertEquals(2, model.prompts.size());
        }
        var missingBreak = scheduleJson();
        ((ObjectNode) missingBreak.path("sessions").get(2)).put("kind", "ACTIVITY");
        var model = new FakeAIModel(prompt -> missingBreak.toString());
        assertThrows(PlanningException.class, () -> agent(model).proposeSchedule(event()));
        assertEquals(2, model.prompts.size());
    }
    private void assertInvalidTasks(String output) {
        var model = new FakeAIModel(prompt -> output);
        assertThrows(PlanningException.class, () -> agent(model).proposeTaskAssignments(event(), List.of()));
        assertEquals(2, model.prompts.size());
    }
    @Test void missingWindowAndTransportFailureDoNotTriggerOutputRetries() {
        var missingEnd = new Event("e", "Name", "Type", "room", START, 10, 100);
        var model = new FakeAIModel(prompt -> { throw new PlanningException("Network failure"); });
        var agent = agent(model);
        assertThrows(IllegalArgumentException.class, () -> agent.proposeSchedule(missingEnd));
        assertTrue(model.prompts.isEmpty());
        assertThrows(PlanningException.class, () -> agent.proposeTaskAssignments(event(), List.of()));
        assertEquals(1, model.prompts.size());
    }
}
