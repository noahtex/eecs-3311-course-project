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
    @Test void cliMutationsUseTheSameJsonStoreAndReleaseItsLock(@org.junit.jupiter.api.io.TempDir java.nio.file.Path directory){
        String id;
        var facade=new EventPlanningFacade(new EventService(),new EventPlanningAgent(new OwnedModel()),q->{throw new AssertionError("No network");},q->{throw new AssertionError("No network");},new com.eventplanner.storage.JsonPlanRepository(directory));
        id=facade.createEvent("CLI event","Social","Campus",java.time.LocalDateTime.parse("2099-01-01T18:00"),80,1000).getId();
        var output=new ByteArrayOutputStream();CLIApp.run(facade,new Scanner("constraints add "+id+"\nConstraint saved from CLI\nexit\n"),new PrintStream(output));
        assertTrue(output.toString().contains("Constraint added"));
        try(var reopened=new EventPlanningFacade(new EventService(),new EventPlanningAgent(new OwnedModel()),q->{throw new AssertionError();},q->{throw new AssertionError();},new com.eventplanner.storage.JsonPlanRepository(directory))){assertEquals(java.util.List.of("Constraint saved from CLI"),reopened.viewEvent(id).getConstraints());}
    }
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
