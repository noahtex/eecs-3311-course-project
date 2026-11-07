package com.eventplanner.view;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.PlanningException;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import com.eventplanner.support.PlanningFixtures;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlanningCLITest {
    @Test
    void cliCollectsIdeasAndConstraintsFusesAndGeneratesBlueprintsThroughController() {
        var event = PlanningFixtures.event();
        var service = new EventService();
        service.createEvent(event);
        var controller = new EventPlanningController(new EventPlanningFacade(service,
                new EventPlanningAgent(prompt -> prompt.contains("F01:") ? PlanningFixtures.concepts(event).toString()
                        : PlanningFixtures.blueprints().toString())));
        String initial = "help\nideas add event-1\n\nLee\n\nOffer a craft station\nideas list event-1\n"
                + "constraints add event-1\nNo alcohol\nconstraints list event-1\nideas fuse event-1\nconcepts list event-1\n";
        // Resolve generated IDs when the CLI requests its next command, just as a user would copy them.
        AtomicReference<String> remaining = new AtomicReference<>(null);
        Readable script = buffer -> {
            String value = remaining.getAndSet("");
            if (value == null) {
                remaining.set("generate");
                buffer.put(initial);
                return initial.length();
            }
            if (value.equals("generate")) {
                remaining.set("view");
                String command = "blueprint generate event-1 " + event.getConcepts().get(0).id()
                        + "\ninvalid\ncommunity\nblueprint list event-1\n";
                buffer.put(command);
                return command.length();
            }
            if (value.equals("view")) {
                String command = "blueprint view event-1 " + event.getBlueprints().get(0).id() + "\nexit\n";
                buffer.put(command);
                return command.length();
            }
            return -1;
        };
        var bytes = new ByteArrayOutputStream();
        new EventPlanningCLI(controller, new Scanner(script), new PrintStream(bytes)).run();
        String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(output.contains("Lee: Offer a craft station"));
        assertTrue(output.contains("No alcohol"));
        assertTrue(output.contains("Concept ID:"));
        assertTrue(output.contains("Blueprint ID:"));
        assertTrue(output.contains("Choose budget, community or attendance"));
        assertTrue(output.contains("UNVERIFIED"));
        assertTrue(output.contains("Feasibility warnings"));
        assertTrue(output.contains("Goodbye."));
        assertEquals(3, event.getOrganizerIdeas().size());
        assertEquals(2, event.getBlueprints().size());
        assertEquals("community", event.getBlueprints().get(0).priority());
    }

    @Test
    void apiErrorsAndUnknownIdsReturnToCommandLoop() {
        var service = new EventService();
        service.createEvent(PlanningFixtures.event());
        var controller = new EventPlanningController(new EventPlanningFacade(service,
                new EventPlanningAgent(prompt -> { throw new PlanningException("Gemini request failed."); })));
        var bytes = new ByteArrayOutputStream();
        new EventPlanningCLI(controller, new Scanner("ideas fuse event-1\nideas list missing\n"
                + "blueprint view event-1 missing\nblueprint generate event-1 missing\nevent list\nexit\n"), new PrintStream(bytes)).run();
        String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(output.contains("Error: Gemini request failed."));
        assertTrue(output.contains("No event found with ID: missing"));
        assertTrue(output.contains("No blueprint found"));
        assertTrue(output.contains("No concept found"));
        assertTrue(output.contains("Name: Club gathering"));
        assertTrue(output.contains("Goodbye."));
    }
}
