package com.eventplanner.facade;

import com.eventplanner.service.EventService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventPlanningFacadeTest {
    private final EventService service = new EventService();
    private final EventPlanningFacade facade = new EventPlanningFacade(service);
    private final LocalDateTime date = LocalDateTime.of(2026, 12, 1, 18, 30);

    @Test
    void createsUniqueEventsUsingInjectedService() {
        var first = facade.createEvent(" Workshop ", "Club", "Campus", date, 20, 100);
        var second = facade.createEvent("Workshop", "Club", "Campus", date, 0, 0);
        assertEquals("Workshop", first.getName());
        assertEquals("Club", first.getType());
        assertEquals("Campus", first.getLocation());
        assertEquals(date, first.getDateTime());
        assertEquals(20, first.getExpectedAttendance());
        assertEquals(100, first.getTotalBudget());
        assertNotEquals(first.getId(), second.getId());
        assertSame(first, service.getEvent(first.getId()));
        assertSame(first, facade.viewEvent(first.getId()));
        assertEquals(2, facade.listEvents().size());
        assertTrue(facade.deleteEvent(first.getId()));
        assertFalse(facade.deleteEvent(first.getId()));
        assertEquals(java.util.List.of(second), facade.listEvents());
    }

    @Test
    void rejectsInvalidDetailsWithoutStoringEvents() {
        assertThrows(IllegalArgumentException.class, () -> facade.createEvent(null, "Club", "Campus", date, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> facade.createEvent(" ", "Club", "Campus", date, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> facade.createEvent("Name", " ", "Campus", date, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> facade.createEvent("Name", "Club", null, date, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> facade.createEvent("Name", "Club", "Campus", null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> facade.createEvent("Name", "Club", "Campus", date, -1, 0));
        for (double budget : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> facade.createEvent("Name", "Club", "Campus", date, 1, budget));
        }
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    void handlesUnknownAndInvalidIds() {
        assertThrows(IllegalArgumentException.class, () -> facade.viewEvent("missing"));
        assertFalse(facade.deleteEvent("missing"));
        for (String id : new String[]{null, "", " "}) {
            assertThrows(IllegalArgumentException.class, () -> facade.viewEvent(id));
            assertThrows(IllegalArgumentException.class, () -> facade.deleteEvent(id));
        }
    }
}
