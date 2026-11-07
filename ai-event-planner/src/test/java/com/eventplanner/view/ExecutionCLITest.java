package com.eventplanner.view;

import com.eventplanner.agent.EventPlanningAgent;
import com.eventplanner.controller.EventPlanningController;
import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.model.*;
import com.eventplanner.service.EventService;
import java.io.*;
import java.util.Scanner;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.ExecutionFixtures.*;

class ExecutionCLITest {
    private final Event event = event();
    private final EventService service = new EventService();
    private final EventPlanningController controller;
    ExecutionCLITest() {
        service.createEvent(event);
        controller = new EventPlanningController(new EventPlanningFacade(service, new EventPlanningAgent(prompt ->
                prompt.contains("F03:") ? tasksJson("alex").toString() : scheduleJson().toString())));
    }
    private String run(String script) {
        var bytes = new ByteArrayOutputStream();
        new EventPlanningCLI(controller, new Scanner(script), new PrintStream(bytes)).run();
        return bytes.toString(java.nio.charset.StandardCharsets.UTF_8);
    }
    @Test void allRequiredTaskCommandsSupportReviewAssignmentStatusAndApproval() {
        String output = run("help\ntasks generate execution\nno\ntasks list execution\nexit\n");
        assertTrue(output.contains("DRAFT task plan"));
        assertTrue(output.contains("tasks assign <eventId> <taskId>"));
        assertTrue(event.getTasks().isEmpty());
        String taskId = controller.listTasks(event.getId()).get(0).id();
        output = run("tasks assign execution " + taskId + "\n\n" + WORK + "\n" + WORK.plusHours(3)
                + "\nno\ntasks status execution " + taskId + "\ninvalid\nIN_PROGRESS\nyes\ntasks list execution\nexit\n");
        assertTrue(output.contains("UNASSIGNED"));
        assertTrue(output.contains("Choose one of"));
        assertTrue(output.contains("Task plan approved and stored"));
        assertEquals(Task.Status.IN_PROGRESS, event.getTasks().get(0).status());
        assertNull(event.getTasks().get(0).assignedUserId());
    }
    @Test void allRequiredScheduleCommandsSupportConflictReportingManualRevisionAndApproval() {
        String output = run("schedule build execution\nno\nschedule view execution\nschedule conflicts execution\nexit\n");
        assertTrue(output.contains("DRAFT schedule"));
        assertTrue(output.contains("SETUP"));
        assertTrue(output.contains("TEARDOWN"));
        assertNull(event.getSchedule());
        Session activity = controller.viewSchedule(event.getId()).sessions().get(1);
        output = run("schedule edit execution " + activity.id() + "\nCraft workshop\nACTIVITY\n"
                + START.plusHours(1) + "\n" + START.plusHours(2)
                + "\nspeaker\nroom\nprojector\nyes\nschedule view execution\nexit\n");
        assertTrue(output.contains("Schedule approved and stored"));
        assertTrue(output.contains("APPROVED schedule"));
        assertEquals("Craft workshop", event.getSchedule().sessions().get(1).title());
    }
    @Test void collectsRealMemberSpeakerResourceAndWindowInputs() {
        String output = run("team set execution\nsam\nSam\nlogistics,craft\ncraft\nyes\n"
                + WORK + "\n" + START + "\nno\n0\n10\nteam list execution\n"
                + "speaker set execution\nguest\nGuest\nyes\n" + START + "\n" + START.plusHours(4) + "\nno\n"
                + "resource set execution\nsecond-room\nyes\n" + START + "\n" + START.plusHours(4) + "\nno\n"
                + "schedule window execution\ninvalid\n" + START.plusHours(5) + "\nexit\n");
        assertTrue(output.contains("Member profile saved"));
        assertTrue(output.contains("skills=[logistics, craft]"));
        assertTrue(output.contains("Speaker saved"));
        assertTrue(output.contains("Resource saved"));
        assertTrue(output.contains("Enter an ISO local datetime"));
        assertEquals(2, event.getPlanningTeam().getMembers().size());
        assertTrue(event.getResourceAvailability().containsKey("second-room"));
        assertEquals(START.plusHours(5), event.getEndDateTime());
    }
    @Test void invalidApprovalAndMissingIdsReturnToCommandLoopAndEofDoesNotApprove() {
        controller.configureResource(event.getId(), "room", java.util.List.of());
        String output = run("schedule build execution\nyes\nschedule conflicts execution\ntasks assign execution missing\n"
                + "schedule edit execution missing\nevent list\nexit\n");
        assertTrue(output.contains("Schedule cannot be approved"));
        assertTrue(output.contains("availability unknown"));
        assertTrue(output.contains("Unknown task ID"));
        assertTrue(output.contains("Unknown session ID"));
        assertTrue(output.contains("Name: Craft workshop"));
        assertNull(event.getSchedule());
        output = run("tasks generate execution\n");
        assertTrue(output.contains("End of input"));
        assertTrue(event.getTasks().isEmpty());
    }
}
