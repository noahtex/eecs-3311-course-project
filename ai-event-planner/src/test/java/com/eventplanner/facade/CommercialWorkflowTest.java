package com.eventplanner.facade;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import com.eventplanner.tool.AITool;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.CommercialFixtures.*;
import static com.eventplanner.support.ExecutionFixtures.event;

class CommercialWorkflowTest {
    private final java.util.List<EventPlanningFacade> facades = new java.util.ArrayList<>();
    private EventPlanningFacade own(EventPlanningFacade facade) {
        facades.add(facade);
        return facade;
    }
    @AfterEach void closeFacades() {
        // Each facade owns its agent, tools and repository, but not the shared EventService.
        facades.forEach(EventPlanningFacade::close);
    }

    @Test void budgetDraftApprovalEditingAndRejectedApprovalPreserveApprovedState() {
        var event = event(); var service = new EventService(); service.createEvent(event);
        var controller = new EventPlanningController(own(new EventPlanningFacade(service, new EventPlanningAgent(prompt -> budgetJson().toString()),
                query -> providers(), query -> providers())));
        controller.configureBudgetNeeds(event.getId(), "CAD", List.of("vegetarian"), "Refreshments");
        Budget draft = controller.generateBudget(event.getId(), "budget");
        assertNull(event.getBudget()); assertTrue(controller.hasBudgetDraft(event.getId()));
        assertEquals(amount("200.00"), controller.budgetTotal(event.getId()));
        assertEquals(amount("300.00"), controller.budgetRemaining(event.getId()));
        controller.approveBudget(event.getId()); assertSame(draft, event.getBudget());
        String itemId = draft.items().get(0).id();
        controller.editBudget(event.getId(), item(itemId, "30", "20", BudgetItem.PriceStatus.ESTIMATED));
        assertTrue(controller.validateBudget(event.getId()).stream().anyMatch(w -> w.startsWith("Over budget")));
        assertThrows(IllegalArgumentException.class, () -> controller.approveBudget(event.getId()));
        assertSame(draft, event.getBudget());
        controller.editBudget(event.getId(), item(itemId, "30", "6", BudgetItem.PriceStatus.CONFIRMED));
        controller.approveBudget(event.getId()); assertEquals(amount("230.00"), controller.budgetTotal(event.getId()));
        assertSame(service.getEvent(event.getId()), event);
        assertFalse(controller.hasBudgetDraft(event.getId()));
        assertEquals(500, event.getTotalBudget());
    }
    @Test void missingCateringServiceProducesExplicitlyUnverifiedBudgetWithoutInventedProviders() {
        var event = event(); var service = new EventService(); service.createEvent(event);
        AITool unavailable = query -> { throw new PlanningException("Not configured"); };
        var facade = own(new EventPlanningFacade(service, new EventPlanningAgent(prompt -> budgetJson().toString()), unavailable, unavailable));
        var budget = facade.generateBudget(event.getId(), "community");
        assertTrue(budget.providers().isEmpty());
        assertTrue(budget.assumptions().stream().anyMatch(w -> w.contains("discovery unavailable")));
        assertNull(budget.catering().get(0).providerId());
        assertThrows(PlanningException.class, () -> facade.researchVenues(event.getId(), "halls"));
        assertTrue(event.getVenueOptions().isEmpty());
    }
    @Test void retrievedAndManualVenuesAreStoredAndComparedByActualIds() {
        var event = event(); var service = new EventService(); service.createEvent(event);
        AtomicReference<String> querySeen = new AtomicReference<>();
        var controller = new EventPlanningController(own(new EventPlanningFacade(service,
                new EventPlanningAgent(prompt -> comparisons(event.getVenueOptions()).toString()), query -> {
                    querySeen.set(query); return new ToolResult("Fake provider", List.of(unknownVenue("retrieved")), List.of());
                }, query -> providers())));
        controller.researchVenues(event.getId(), "workshop halls");
        assertTrue(querySeen.get().contains(event.getLocation()));
        controller.addVenue(event.getId(), knownVenue("manual"));
        assertEquals(2, controller.listVenues(event.getId()).size());
        controller.configureVenueNeeds(event.getId(), true, List.of("projector"));
        var comparisons = controller.compareVenues(event.getId());
        assertEquals("manual", comparisons.get(0).venueId());
        assertEquals(comparisons, event.getVenueComparisons());
        assertThrows(UnsupportedOperationException.class, () -> controller.listVenues(event.getId()).clear());
    }
    @Test void failedResearchAndMalformedGenerationKeepPreviousDataAndDrafts() {
        var event = event(); var service = new EventService(); service.createEvent(event);
        event.addVenueOptions(List.of(knownVenue("existing")));
        AtomicReference<String> output = new AtomicReference<>(budgetJson().toString());
        var facade = own(new EventPlanningFacade(service, new EventPlanningAgent(prompt -> output.get()),
                query -> { throw new PlanningException("External provider failed"); }, query -> providers()));
        Budget draft = facade.generateBudget(event.getId(), "budget");
        output.set("{}");
        assertThrows(PlanningException.class, () -> facade.generateBudget(event.getId(), "budget"));
        assertSame(draft, facade.viewBudget(event.getId()));
        assertThrows(PlanningException.class, () -> facade.researchVenues(event.getId(), "query"));
        assertEquals("existing", event.getVenueOptions().get(0).id());
        facade.discardBudget(event.getId()); assertFalse(facade.hasBudgetDraft(event.getId()));
    }
    @Test void unknownAmountsCannotBeApprovedAndUnknownEventsDoNotCallAiOrTools() {
        var event = event(); var service = new EventService(); service.createEvent(event);
        var output = budgetJson();
        ((ObjectNode) output.path("items").get(0)).put("priceStatus", "UNKNOWN").putNull("unitCost");
        var facade = own(new EventPlanningFacade(service, new EventPlanningAgent(prompt -> output.toString()), query -> providers(), query -> providers()));
        facade.generateBudget(event.getId(), "budget");
        assertThrows(IllegalArgumentException.class, () -> facade.approveBudget(event.getId()));
        assertNull(event.getBudget());
        var isolated = own(new EventPlanningFacade(service, new EventPlanningAgent(prompt -> fail("Unexpected AI call")),
                query -> fail("Unexpected tool call"), query -> fail("Unexpected tool call")));
        assertThrows(IllegalArgumentException.class, () -> isolated.generateBudget("missing", "budget"));
        assertThrows(IllegalArgumentException.class, () -> isolated.researchVenues("missing", "query"));
        assertThrows(IllegalArgumentException.class, () -> isolated.compareVenues("missing"));
    }
}
