package com.eventplanner.view;

import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventPlanningCLITest {
    private final EventPlanningController controller = new EventPlanningController(
            new EventPlanningFacade(new EventService()));

    private String run(String commands) {
        var bytes = new ByteArrayOutputStream();
        new EventPlanningCLI(controller, new Scanner(commands), new PrintStream(bytes)).run();
        return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Test
    void createsAndListsEventAfterRetryingInvalidFields() {
        String output = run("help\nevent create\n\nWorkshop\nClub\nCampus\n2026-02-30T18:30\n2026-12-01T18:30\nno\n-1\n2147483648\n10\nNaN\nInfinity\n-1\nabc\n20.5\nevent list\nexit\n");
        assertTrue(output.contains("Error: This field is required."));
        assertTrue(output.contains("Enter a valid date/time"));
        assertTrue(output.contains("Attendance must be"));
        assertTrue(output.contains("Budget must be"));
        assertTrue(output.contains("event view <id>"));
        assertTrue(output.contains("Event created. ID:"));
        assertTrue(output.contains("Name: Workshop"));
        assertTrue(output.contains("Budget: 20.50"));
        assertTrue(output.contains("Goodbye."));
        assertEquals(1, controller.listEvents().size());
    }

    @Test
    void viewsCancelsThenConfirmsDeletion() {
        var event = controller.createEvent("Workshop", "Club", "Campus", LocalDateTime.now(), 10, 20);
        String id = event.getId();
        String output = run("event view " + id + "\nevent delete " + id + "\nno\nevent view " + id
                + "\nevent delete " + id + "\nyes\nevent list\nexit\n");
        assertTrue(output.contains("Name: Workshop"));
        assertTrue(output.contains("Deletion cancelled."));
        assertTrue(output.contains("Event deleted."));
        assertTrue(output.contains("No events."));
        assertTrue(controller.listEvents().isEmpty());
    }

    @Test
    void recoversFromUnknownCommandsIdsAndMissingArguments() {
        String output = run("oops\nevent view\nevent delete\nevent view missing\nevent delete missing\nexit\n");
        assertTrue(output.contains("Unknown command or invalid arguments"));
        assertTrue(output.contains("No event found with ID: missing"));
        assertTrue(output.contains("Goodbye."));
    }

    @Test
    void endOfInputCancelsIncompleteCreation() {
        assertTrue(run("event create\nWorkshop\n").contains("End of input"));
        assertTrue(controller.listEvents().isEmpty());
    }

    @Test
    void endOfInputDoesNotConfirmDeletion() {
        var event = controller.createEvent("Workshop", "Club", "Campus", LocalDateTime.now(), 10, 20);
        assertTrue(run("event delete " + event.getId() + "\n").contains("End of input"));
        assertSame(event, controller.viewEvent(event.getId()));
    }
}
