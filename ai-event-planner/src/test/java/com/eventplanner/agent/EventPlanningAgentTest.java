package com.eventplanner.agent;

import com.eventplanner.ai.PlanningException;
import com.eventplanner.model.Event;
import com.eventplanner.strategy.*;
import com.eventplanner.support.PlanningFixtures;
import com.eventplanner.support.PlanningFixtures.FakeAIModel;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

class EventPlanningAgentTest {
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

    @Test
    void fusionProducesTraceableStructuredAlternativesWithConflictsAndUncertainty() {
        Event event = PlanningFixtures.event();
        var model = new FakeAIModel(prompt -> PlanningFixtures.concepts(event).toString());
        var concepts = agent(model).synthesizeIdeas(event);
        assertEquals(2, concepts.size());
        assertNotEquals(concepts.get(0).id(), concepts.get(1).id());
        assertEquals(2, concepts.get(0).incorporatedIdeas().size());
        assertFalse(concepts.get(0).themes().isEmpty());
        assertFalse(concepts.get(0).conflicts().isEmpty());
        assertFalse(concepts.get(0).compromises().isEmpty());
        assertTrue(concepts.get(0).uncertainties().contains(EventPlanningAgent.UNVERIFIED));
        assertTrue(model.prompts.get(0).contains("Alex"));
        assertTrue(model.prompts.get(0).contains("Step-free access"));
        assertTrue(model.prompts.get(0).contains("budgetCeiling"));
        assertThrows(UnsupportedOperationException.class, () -> concepts.clear());
    }

    @Test
    void blueprintGenerationAppliesEachStrategyAndIncludesStructuredSections() {
        Event event = PlanningFixtures.event();
        var model = new FakeAIModel(prompt -> prompt.contains("F01:")
                ? PlanningFixtures.concepts(event).toString() : PlanningFixtures.blueprints().toString());
        var agent = agent(model);
        event.addConcepts(agent.synthesizeIdeas(event));
        for (PlanningStrategy strategy : List.of(new BudgetPriorityStrategy(), new CommunityPriorityStrategy(), new AttendancePriorityStrategy())) {
            agent.setPlanningStrategy(strategy);
            var blueprints = agent.generateBlueprints(event, event.getConcepts().get(0));
            assertEquals(2, blueprints.size());
            var blueprint = blueprints.get(0);
            assertEquals(strategy.name(), blueprint.priority());
            assertEquals(event.getConcepts().get(0).id(), blueprint.conceptId());
            assertFalse(blueprint.agenda().isEmpty());
            assertFalse(blueprint.venueRequirements().isEmpty());
            assertFalse(blueprint.cateringAssumptions().isEmpty());
            assertFalse(blueprint.resources().isEmpty());
            assertFalse(blueprint.checklist().isEmpty());
            assertFalse(blueprint.assumptions().isEmpty());
            assertEquals(400, blueprint.estimatedBudget());
            assertTrue(blueprint.feasibilityWarnings().contains(EventPlanningAgent.UNVERIFIED));
            assertTrue(model.prompts.get(model.prompts.size() - 1).contains(strategy.buildGuidance(event)));
        }
        assertEquals(3, model.prompts.subList(1, 4).stream().distinct().count());
        assertThrows(IllegalArgumentException.class, () -> PlanningStrategy.fromName("unknown"));
    }

    @Test
    void deterministicWarningsExposeBudgetAttendanceAndCapacityConflicts() {
        Event event = PlanningFixtures.event();
        ObjectNode response = PlanningFixtures.blueprints();
        ((ObjectNode) response.path("blueprints").get(0)).put("estimatedBudget", 600)
                .put("plannedAttendance", 25).put("requiredCapacity", 20);
        var model = new FakeAIModel(prompt -> prompt.contains("F01:")
                ? PlanningFixtures.concepts(event).toString() : response.toString());
        var agent = agent(model);
        event.addConcepts(agent.synthesizeIdeas(event));
        var warnings = agent.generateBlueprints(event, event.getConcepts().get(0)).get(0).feasibilityWarnings();
        assertTrue(warnings.stream().anyMatch(w -> w.startsWith("Budget conflict:")));
        assertTrue(warnings.stream().anyMatch(w -> w.startsWith("Attendance conflict:")));
        assertTrue(warnings.stream().anyMatch(w -> w.startsWith("Capacity conflict:")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("free-text constraints")));
    }

    @Test
    void retriesMalformedOutputExactlyOnceAndCanRecover() {
        Event event = PlanningFixtures.event();
        AtomicInteger calls = new AtomicInteger();
        var model = new FakeAIModel(prompt -> calls.getAndIncrement() == 0 ? "not JSON" : PlanningFixtures.concepts(event).toString());
        assertEquals(2, agent(model).synthesizeIdeas(event).size());
        assertEquals(2, model.prompts.size());
        assertTrue(model.prompts.get(1).contains("RETRY:"));
    }

    @Test
    void rejectsMissingWrongTypedEmptyUnknownSourceAndDuplicateAlternatives() {
        Event event = PlanningFixtures.event();
        ObjectNode wrongId = PlanningFixtures.concepts(event);
        ((ObjectNode) wrongId.path("concepts").get(0)).putArray("incorporatedIdeas").add("invented");
        ObjectNode empty = PlanningFixtures.concepts(event);
        ((ObjectNode) empty.path("concepts").get(0)).putArray("themes");
        ObjectNode missingCompromise = PlanningFixtures.concepts(event);
        ((ObjectNode) missingCompromise.path("concepts").get(0)).putArray("compromises");
        ObjectNode duplicate = PlanningFixtures.concepts(event);
        ((ObjectNode) duplicate.path("concepts").get(1)).put("title", "Concept 0");
        ObjectNode uncovered = PlanningFixtures.concepts(event);
        uncovered.path("concepts").forEach(node -> ((ObjectNode) node).putArray("incorporatedIdeas")
                .add(event.getOrganizerIdeas().get(0).id()));
        for (String invalid : List.of("null", "{}", "{\"concepts\":[]}", "{\"concepts\":[{}]}",
                "{\"concepts\":[{},{}]}", "{\"concepts\":\"wrong type\"}", wrongId.toString(),
                empty.toString(), missingCompromise.toString(), duplicate.toString(), uncovered.toString(),
                PlanningFixtures.concepts(event) + " {}", "{\"concepts\":[],\"concepts\":[]}")) {
            var model = new FakeAIModel(prompt -> invalid);
            assertThrows(PlanningException.class, () -> agent(model).synthesizeIdeas(event));
            assertEquals(2, model.prompts.size(), invalid);
        }
    }

    @Test
    void rejectsInvalidBlueprintsIncludingNumericAndMissingFields() {
        Event event = PlanningFixtures.event();
        var setup = agent(prompt -> PlanningFixtures.concepts(event).toString());
        event.addConcepts(setup.synthesizeIdeas(event));
        for (String field : List.of("agenda", "venueRequirements", "cateringAssumptions", "resources", "checklist",
                "assumptions", "feasibilityWarnings", "estimatedBudget", "plannedAttendance", "requiredCapacity")) {
            ObjectNode response = PlanningFixtures.blueprints();
            ((ObjectNode) response.path("blueprints").get(1)).remove(field);
            var model = new FakeAIModel(prompt -> response.toString());
            assertThrows(PlanningException.class, () -> agent(model).generateBlueprints(event, event.getConcepts().get(0)));
            assertEquals(2, model.prompts.size());
        }
        for (double value : new double[]{-1, 0.5, 2147483648.0}) {
            ObjectNode response = PlanningFixtures.blueprints();
            ((ObjectNode) response.path("blueprints").get(0)).put("plannedAttendance", value);
            assertThrows(PlanningException.class, () -> agent(prompt -> response.toString())
                    .generateBlueprints(event, event.getConcepts().get(0)));
        }
        ObjectNode response = PlanningFixtures.blueprints();
        ((ObjectNode) response.path("blueprints").get(0)).put("estimatedBudget", -1);
        assertThrows(PlanningException.class, () -> agent(prompt -> response.toString())
                .generateBlueprints(event, event.getConcepts().get(0)));
    }

    @Test
    void rejectsMissingIdeasAndInvalidKnownConstraintsBeforeCallingModel() {
        var model = new FakeAIModel(prompt -> fail("AI should not be invoked"));
        var agent = agent(model);
        Event valid = PlanningFixtures.event();
        var date = valid.getDateTime();
        for (Event event : List.of(
                new Event("id", "Name", "Type", "Location", date, 30, 500),
                new Event("id", "", "Type", "Location", date, 30, 500),
                new Event("id", "Name", "", "Location", date, 30, 500),
                new Event("id", "Name", "Type", null, date, 30, 500),
                new Event("id", "Name", "Type", "Location", null, 30, 500),
                new Event("id", "Name", "Type", "Location", date, 0, 500),
                new Event("id", "Name", "Type", "Location", date, -1, 500),
                new Event("id", "Name", "Type", "Location", date, 30, -1),
                new Event("id", "Name", "Type", "Location", date, 30, Double.NaN),
                new Event("id", "Name", "Type", "Location", date, 30, Double.POSITIVE_INFINITY))) {
            assertThrows(IllegalArgumentException.class, () -> agent.synthesizeIdeas(event));
        }
        assertThrows(IllegalArgumentException.class, () -> agent.synthesizeIdeas(null));
        assertThrows(IllegalArgumentException.class, () -> agent.generateBlueprints(valid, null));
        assertTrue(model.prompts.isEmpty());
    }

    @Test
    void transportFailureIsNotRetriedAsInvalidOutput() {
        var model = new FakeAIModel(prompt -> { throw new PlanningException("API unavailable"); });
        assertThrows(PlanningException.class, () -> agent(model).synthesizeIdeas(PlanningFixtures.event()));
        assertEquals(1, model.prompts.size());
    }
}
