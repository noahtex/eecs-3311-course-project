package com.eventplanner;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.ai.AIModel;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CLIAppLifecycleTest {
    private static final class OwnedModel implements AIModel {
        int closes;
        @Override public String generate(String prompt) { return fail("No network operations expected"); }
        @Override public void close() { closes++; }
    }

    @Test
    void exitAndEndOfInputCloseFacadeAgentAndModel() {
        for (String commands : new String[]{"exit\n", ""}) {
            OwnedModel model = new OwnedModel();
            var facade = new EventPlanningFacade(new EventService(), new EventPlanningAgent(model));
            var output = new ByteArrayOutputStream();
            CLIApp.run(facade, new Scanner(commands), new PrintStream(output));
            assertEquals(1, model.closes);
            assertTrue(output.toString().contains("Goodbye."));
        }
    }

    @Test
    void unexpectedFailureClosesResourcesWithoutClosingBorrowedInput() {
        OwnedModel model = new OwnedModel();
        var facade = new EventPlanningFacade(new EventService(), new EventPlanningAgent(model));
        Readable failedInput = buffer -> { throw new IllegalStateException("input failure"); };
        Scanner input = new Scanner(failedInput);
        var failure = assertThrows(IllegalStateException.class,
                () -> CLIApp.run(facade, input, new PrintStream(new ByteArrayOutputStream())));
        assertEquals("input failure", failure.getMessage());
        assertEquals(1, model.closes);
        // A closed Scanner would reject reset(). Ownership remains with the caller.
        assertDoesNotThrow(input::reset);
        input.close();
    }
}
