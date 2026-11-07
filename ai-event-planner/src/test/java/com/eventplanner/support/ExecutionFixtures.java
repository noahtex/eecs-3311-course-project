package com.eventplanner.support;

import com.eventplanner.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDateTime;
import java.util.List;

public final class ExecutionFixtures {
    public static final LocalDateTime START = LocalDateTime.of(2099, 1, 10, 9, 0);
    public static final LocalDateTime WORK = START.minusDays(2);
    private ExecutionFixtures() { }
    public static Event event() {
        Event event = new Event("execution", "Craft workshop", "Workshop", "room", START, 30, 500);
        event.setEndDateTime(START.plusHours(4));
        event.addIdea("Planner", "Accessible craft workshop with a welcome break");
        event.getPlanningTeam().addMember(member("alex", 0, 10, List.of(new TimeSlot(WORK.minusDays(2), START))));
        event.putSpeaker(new Speaker("speaker", "Presenter", List.of(new TimeSlot(START, START.plusHours(4)))));
        event.putResourceAvailability("room", List.of(new TimeSlot(START, START.plusHours(4))));
        event.putResourceAvailability("projector", List.of(new TimeSlot(START, START.plusHours(4))));
        return event;
    }
    public static User member(String id, double workload, double capacity, List<TimeSlot> slots) {
        User user = new User(id, "Alex", "not-sent@example.invalid", "Planner");
        user.configurePlanning(List.of("logistics"), List.of("craft"), slots, workload, capacity);
        return user;
    }
    public static Task task(String id, String assignee) {
        return new Task(id, "Arrange craft supplies", "Confirm and organize supplies", assignee,
                WORK, WORK.plusHours(3), 2, List.of("logistics"), Task.Priority.HIGH, Task.Status.TODO);
    }
    public static Schedule schedule() {
        return new Schedule("agenda", List.of(
                session("setup", Session.Kind.SETUP, 0, 1, null, "room", List.of()),
                session("activity", Session.Kind.ACTIVITY, 1, 2, "speaker", "room", List.of("projector")),
                session("break", Session.Kind.BREAK, 2, 3, null, "room", List.of()),
                session("teardown", Session.Kind.TEARDOWN, 3, 4, null, "room", List.of())));
    }
    public static Session session(String id, Session.Kind kind, int start, int end, String speaker, String space, List<String> resources) {
        return new Session(id, id, kind, new TimeSlot(START.plusHours(start), START.plusHours(end)), speaker, space, resources);
    }
    public static ObjectNode tasksJson(String assignee) {
        ObjectNode root = new ObjectMapper().createObjectNode();
        var array = root.putArray("tasks");
        for (int i = 0; i < 2; i++) {
            var node = array.addObject().put("title", i == 0 ? "Arrange craft supplies" : "Prepare accessible workshop space")
                    .put("description", "Coordinate workshop logistics with the planner").put("assignedUserId", assignee)
                    .put("workStart", WORK.plusDays(i).toString()).put("deadline", WORK.plusDays(i).plusHours(3).toString())
                    .put("effortHours", 2).put("priority", "HIGH");
            node.putArray("requiredSkills").add("logistics");
        }
        return root;
    }
    public static ObjectNode scheduleJson() {
        ObjectNode root = new ObjectMapper().createObjectNode();
        var array = root.putArray("sessions");
        schedule().sessions().forEach(session -> {
            var node = array.addObject().put("title", session.title()).put("kind", session.kind().name())
                    .put("start", session.slot().start().toString()).put("end", session.slot().end().toString())
                    .put("speakerId", session.speakerId()).put("space", session.space());
            var resources = node.putArray("resources");
            session.resources().forEach(resources::add);
        });
        return root;
    }
}
