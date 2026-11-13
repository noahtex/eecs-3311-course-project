package com.eventplanner.agent;

import com.eventplanner.ai.PlanningException;
import com.eventplanner.model.*;
import com.eventplanner.strategy.CommunityPriorityStrategy;
import com.eventplanner.support.PlanningFixtures.FakeAIModel;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.CommercialFixtures.*;
import static com.eventplanner.support.ExecutionFixtures.event;

class CommercialAgentTest {
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

    @Test void preservesDecimalMoneyWithoutBinaryFloatingPointConversion() {
        var output = budgetJson();
        ((ObjectNode) output.path("items").get(0)).put("unitCost", amount("123456789012345.67"));
        var budget = agent(prompt -> output.toString()).planBudgetAndCatering(event(), providers());
        assertEquals(amount("123456789012345.67"), budget.items().get(0).unitCost());
    }
    @Test void budgetIncludesDietaryContextProviderEvidenceAndPriorityAndForcesEstimates() {
        var event = event(); event.configureBudgetNeeds("CAD", List.of("vegetarian", "nut allergy"), "Buffet for attendees");
        var output = budgetJson();
        ((ObjectNode) output.path("catering").get(0)).put("providerId", "provider-1");
        var model = new FakeAIModel(prompt -> output.toString());
        var agent = agent(model); agent.setPlanningStrategy(new CommunityPriorityStrategy());
        Budget budget = agent.planBudgetAndCatering(event, providers());
        assertEquals("community", budget.priority());
        assertEquals(2, budget.items().size());
        assertTrue(budget.items().stream().allMatch(item -> item.priceStatus() == BudgetItem.PriceStatus.ESTIMATED));
        assertEquals("provider-1", budget.catering().get(0).providerId());
        for (String fragment : List.of("vegetarian", "nut allergy", "Buffet", "provider-1", "community", "UNKNOWN", "Craft workshop"))
            assertTrue(model.prompts.get(0).contains(fragment), fragment);
        assertNull(event.getBudget());
    }
    @Test void rejectsNegativeNumbersConfirmedAiPricesMissingFieldsAndInventedProvidersAfterOneRetry() {
        List<ObjectNode> invalid = new ArrayList<>();
        for (String field : List.of("category", "description", "quantity", "unitCost", "priceStatus")) {
            var output = budgetJson(); ((ObjectNode) output.path("items").get(0)).remove(field); invalid.add(output);
        }
        for (String field : List.of("quantity", "unitCost")) {
            var output = budgetJson(); ((ObjectNode) output.path("items").get(0)).put(field, -1); invalid.add(output);
        }
        var confirmed = budgetJson(); ((ObjectNode) confirmed.path("items").get(0)).put("priceStatus", "CONFIRMED"); invalid.add(confirmed);
        var unknownAsZero = budgetJson(); ((ObjectNode) unknownAsZero.path("items").get(0)).put("priceStatus", "UNKNOWN"); invalid.add(unknownAsZero);
        var invented = budgetJson(); ((ObjectNode) invented.path("catering").get(0)).put("providerId", "invented"); invalid.add(invented);
        for (var output : invalid) {
            var model = new FakeAIModel(prompt -> output.toString());
            assertThrows(PlanningException.class, () -> agent(model).planBudgetAndCatering(event(), providers()));
            assertEquals(2, model.prompts.size());
        }
    }
    @Test void retriesMalformedBudgetOnceAndDoesNotRetryTransportFailure() {
        var count = new AtomicInteger();
        var model = new FakeAIModel(prompt -> count.getAndIncrement() == 0 ? "not JSON" : budgetJson().toString());
        assertNotNull(agent(model).planBudgetAndCatering(event(), providers()));
        assertEquals(2, model.prompts.size());
        var failed = new FakeAIModel(prompt -> { throw new PlanningException("Transport unavailable"); });
        assertThrows(PlanningException.class, () -> agent(failed).planBudgetAndCatering(event(), providers()));
        assertEquals(1, failed.prompts.size());
    }
    @Test void comparisonsUseEveryRealCandidateAndPreserveDeterministicScoresAndEvidence() {
        var event = event(); event.configureVenueNeeds(true, List.of("projector"));
        event.addVenueOptions(List.of(unknownVenue("unknown"), knownVenue("known")));
        var model = new FakeAIModel(prompt -> comparisons(event.getVenueOptions()).toString());
        var result = agent(model).compareVenues(event);
        assertEquals("known", result.get(0).venueId());
        assertTrue(model.prompts.get(0).contains("PLANNER_CONFIRMED"));
        assertTrue(model.prompts.get(0).contains("UNKNOWN"));
        assertNull(event.getVenueOptions().get(0).capacity().value());
        assertTrue(result.get(1).warnings().stream().anyMatch(w -> w.contains("unknown")));
    }
    @Test void rejectsInventedMissingOrDuplicateVenueIdsAndStopsAfterOneRetry() {
        var event = event(); event.addVenueOptions(List.of(knownVenue("known"), unknownVenue("unknown")));
        for (String output : List.of("{}", "{\"comparisons\":[]}",
                "{\"comparisons\":[{\"venueId\":\"invented\",\"analysis\":\"text\",\"tradeOffs\":[\"x\"]}]}")) {
            var model = new FakeAIModel(prompt -> output);
            assertThrows(PlanningException.class, () -> agent(model).compareVenues(event));
            assertEquals(2, model.prompts.size());
        }
        var duplicate = comparisons(event.getVenueOptions());
        ((ObjectNode) duplicate.path("comparisons").get(1)).put("venueId", "known");
        assertThrows(PlanningException.class, () -> agent(prompt -> duplicate.toString()).compareVenues(event));
    }
}
