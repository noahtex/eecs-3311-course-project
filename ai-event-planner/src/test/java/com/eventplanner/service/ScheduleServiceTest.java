package com.eventplanner.service;

import com.eventplanner.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.ExecutionFixtures.*;

class ScheduleServiceTest {
    private final ScheduleService service = new ScheduleService();
    @Test void validAgendaStoresAndAdjacentSlotsDoNotConflict() {
        Event event = event();
        Schedule agenda = schedule();
        assertFalse(agenda.sessions().get(0).slot().overlaps(agenda.sessions().get(1).slot()));
        assertTrue(service.detectConflicts(event, agenda).isEmpty());
        service.saveSchedule(event, agenda);
        assertSame(agenda, event.getSchedule());
    }
    @Test void detectsActualOverlapsForSpeakerSpaceAndExclusiveResource() {
        Session first = session("a", Session.Kind.ACTIVITY, 1, 3, "speaker", "Room", List.of("Projector"));
        for (Session second : List.of(
                session("b", Session.Kind.ACTIVITY, 2, 4, "speaker", "other", List.of()),
                session("b", Session.Kind.ACTIVITY, 2, 4, null, "room", List.of()),
                session("b", Session.Kind.ACTIVITY, 2, 4, null, "other", List.of("projector")))) {
            assertFalse(service.detectConflicts(new Schedule("s", List.of(first, second))).isEmpty());
        }
        var independent = session("b", Session.Kind.ACTIVITY, 2, 4, "other-speaker", "other", List.of());
        assertTrue(service.detectConflicts(new Schedule("s", List.of(first, independent))).isEmpty());
        var later = session("b", Session.Kind.ACTIVITY, 3, 4, "speaker", "room", List.of("projector"));
        assertTrue(service.detectConflicts(new Schedule("s", List.of(first, later))).isEmpty());
    }
    @Test void flagsUnavailableUnknownSpeakersAndUnknownResources() {
        Event event = event();
        event.putSpeaker(new Speaker("speaker", "Unavailable", List.of(new TimeSlot(START, START.plusHours(1)))));
        assertTrue(service.detectConflicts(event, schedule()).stream().anyMatch(s -> s.contains("speaker is unavailable")));
        event.putSpeaker(new Speaker("speaker", "Unknown", List.of()));
        assertTrue(service.detectConflicts(event, schedule()).stream().anyMatch(s -> s.contains("speaker availability is unknown")));
        Event missing = new Event("e", "E", "T", "room", START, 10, 100);
        missing.setEndDateTime(START.plusHours(4));
        var issues = service.detectConflicts(missing, schedule());
        assertTrue(issues.stream().anyMatch(s -> s.contains("unknown speaker")));
        assertTrue(issues.stream().anyMatch(s -> s.contains("availability unknown for resource")));
    }
    @Test void blocksOutOfBoundsMissingKindsUnavailableResourcesAndInvalidOrder() {
        Event event = event();
        event.setEndDateTime(START.plusHours(3));
        assertTrue(service.detectConflicts(event, schedule()).stream().anyMatch(s -> s.contains("outside event")));
        assertThrows(IllegalArgumentException.class, () -> service.saveSchedule(event, schedule()));
        assertNull(event.getSchedule());
        event.setEndDateTime(START.plusHours(4));
        event.putResourceAvailability("projector", List.of(new TimeSlot(START, START.plusHours(1))));
        assertTrue(service.detectConflicts(event, schedule()).stream().anyMatch(s -> s.contains("resource unavailable")));
        var incomplete = new Schedule("s", List.of(session("a", Session.Kind.ACTIVITY, 0, 1, null, "room", List.of())));
        assertTrue(service.detectConflicts(event, incomplete).stream().anyMatch(s -> s.contains("missing SETUP")));
        assertTrue(service.detectConflicts(event, incomplete).stream().anyMatch(s -> s.contains("must follow setup")));
        assertThrows(IllegalArgumentException.class, () -> new TimeSlot(START.plusHours(1), START));
    }
}
