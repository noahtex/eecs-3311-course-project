package com.eventplanner.view;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import com.eventplanner.tool.AITool;
import java.io.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.CommercialFixtures.*;
import static com.eventplanner.support.ExecutionFixtures.event;

class CommercialCLITest {
    private final Event event = event();
    private EventPlanningController controller(AITool venueTool) {
        var service = new EventService(); service.createEvent(event);
        return new EventPlanningController(new EventPlanningFacade(service, new EventPlanningAgent(prompt ->
                prompt.contains("F05:") ? budgetJson().toString() : comparisons(event.getVenueOptions()).toString()), venueTool, query -> providers()));
    }
    private String run(EventPlanningController controller, String commands) {
        var bytes = new ByteArrayOutputStream();
        new EventPlanningCLI(controller, new Scanner(commands), new PrintStream(bytes)).run();
        return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test void requiredBudgetCommandsGenerateViewEditValidateAndApproveThroughController() {
        var controller = controller(query -> providers());
        String output = run(controller, "help\nbudget generate execution\nCAD\nvegetarian\nRefreshments\nbudget\nbudget view execution\nbudget validate execution\nexit\n");
        assertTrue(output.contains("DRAFT budget")); assertTrue(output.contains("Known subtotal"));
        assertTrue(output.contains("UNVERIFIED catering recommendation")); assertNull(event.getBudget());
        String itemId = controller.viewBudget(event.getId()).items().get(0).id();
        output = run(controller, "budget edit execution " + itemId + "\nCatering\nQuoted refreshments\n-1\n30\nCONFIRMED\n6\nPlanner quote reference\n"
                + "budget validate execution\nbudget approve execution\nbudget view execution\nexit\n");
        assertTrue(output.contains("Enter a positive quantity"));
        assertTrue(output.contains("Budget approved and stored")); assertTrue(output.contains("APPROVED budget"));
        assertEquals(amount("230.00"), controller.budgetTotal(event.getId()));
    }
    @Test void manualEntryWorksAfterExternalFailureAndCanBeComparedWithoutPlaces() {
        var controller = controller(query -> { throw new PlanningException("Places unavailable. Use venues add."); });
        String output = run(controller, "venues research execution\nworkshop rooms\nvenues add execution\nPlanner supplied venue\nroom, Toronto\nPlanner estimate\n"
                + "ESTIMATED\n\n\n\n\nyes\nvenues list execution\nvenues compare execution\nyes\nprojector\nexit\n");
        assertTrue(output.contains("Places unavailable"));
        assertTrue(output.contains("Manual venue candidate stored"));
        assertTrue(output.contains("UNKNOWN")); assertTrue(output.contains("UNVERIFIED AI interpretation"));
        assertEquals(1, event.getVenueOptions().size());
        assertEquals(1, event.getVenueComparisons().size());
        assertNull(event.getVenueOptions().get(0).rentalCost().value());
    }
    @Test void liveResearchUsesInjectedToolAndListsStoredProviderData() {
        var controller = controller(query -> new ToolResult("Fake external tool", List.of(unknownVenue("retrieved")), List.of("No rental quote")));
        String output = run(controller, "venues research execution\nhalls\nvenues list execution\nvenues compare execution\nno\n\nexit\n");
        assertTrue(output.contains("retrieved")); assertTrue(output.contains("No rental quote"));
        assertTrue(output.contains("Rank 1")); assertTrue(output.contains("no venue is selected or booked"));
    }
    @Test void invalidApprovalReturnsToLoopAndEofDoesNotStoreManualEntry() {
        var controller = controller(query -> providers());
        controller.generateBudget(event.getId(), "budget");
        String id = controller.viewBudget(event.getId()).items().get(0).id();
        controller.editBudget(event.getId(), item(id, "30", "30", BudgetItem.PriceStatus.ESTIMATED));
        String output = run(controller, "budget approve execution\nevent list\nbudget edit execution missing\nvenues add execution\nUnfinished venue\n");
        assertTrue(output.contains("Cannot approve an over-budget plan"));
        assertTrue(output.contains("Name: Craft workshop")); assertTrue(output.contains("Unknown budget item ID"));
        assertTrue(output.contains("End of input")); assertNull(event.getBudget()); assertTrue(event.getVenueOptions().isEmpty());
    }
}
