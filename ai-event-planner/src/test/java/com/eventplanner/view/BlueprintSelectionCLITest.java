package com.eventplanner.view;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.Event;
import com.eventplanner.service.EventService;
import java.io.*;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import static com.eventplanner.support.BlueprintFixtures.addAlternatives;
import static com.eventplanner.support.ExecutionFixtures.event;
import static org.junit.jupiter.api.Assertions.*;

class BlueprintSelectionCLITest {
    @Test void selectListViewApproveAndCancellationUseSharedBackend() {
        Event event = event(); var alternatives = addAlternatives(event); var events = new EventService(); events.createEvent(event);
        try (var facade = new EventPlanningFacade(events, new EventPlanningAgent(p -> { throw new AssertionError("No AI"); }))) {
            var controller = new EventPlanningController(facade);
            String prefix = "blueprint select "+event.getId()+" ";
            var bytes = new ByteArrayOutputStream();
            String commands = "help\nblueprint approve "+event.getId()+"\n"+prefix+"missing\n"
                    + prefix+alternatives.getFirst().id()+"\nblueprint approve "+event.getId()+"\nno\n"
                    + "blueprint list "+event.getId()+"\nblueprint approve "+event.getId()+"\nyes\n"
                    + prefix+alternatives.getLast().id()+"\nblueprint approve "+event.getId()+"\nno\n"
                    + "blueprint view "+event.getId()+" "+alternatives.getFirst().id()+"\nblueprint list "+event.getId()+"\nexit\n";
            new EventPlanningCLI(controller, new Scanner(commands), new PrintStream(bytes)).run();
            String output = bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(output.contains("blueprint select <eventId> <blueprintId>"));
            assertTrue(output.contains("Select a blueprint before approving"));
            assertTrue(output.contains("belonging to this event"));
            assertTrue(output.contains("SELECTED - awaiting approval"));
            assertTrue(output.contains("Approval cancelled; current execution blueprint unchanged"));
            assertTrue(output.contains("Execution status: APPROVED FOR EXECUTION"));
            assertTrue(output.contains("Approved execution blueprint: Quiet workshop"));
            assertTrue(output.contains("Selected for approval: Loud concert"));
            assertEquals(alternatives.getFirst(), event.getApprovedBlueprint());
            assertEquals(alternatives.getLast().id(), event.getSelectedBlueprintId());
            assertEquals(alternatives, event.getBlueprints());
        }
    }
}
