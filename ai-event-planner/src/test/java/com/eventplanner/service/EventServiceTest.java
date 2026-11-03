package com.eventplanner.service;

import com.eventplanner.model.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EventServiceTest {

    private EventService service;

    @BeforeEach
    void setUp() {
        service = new EventService();
    }

    @Test
    void createEventStoresEvent() {
        Event event = event("event-1");

        service.createEvent(event);

        assertSame(event, service.getEvent("event-1"));
        assertEquals(List.of(event), service.getAllEvents());
    }

    @Test
    void createEventRejectsDuplicateIdAndPreservesOriginal() {
        Event original = event(new String("event-1"));
        Event duplicate = event(new String("event-1"));
        service.createEvent(original);

        assertThrows(IllegalArgumentException.class,
                () -> service.createEvent(duplicate));

        assertSame(original, service.getEvent("event-1"));
        assertEquals(List.of(original), service.getAllEvents());
    }

    @Test
    void createEventRejectsNullAndPreservesExistingEvents() {
        Event original = event("event-1");
        service.createEvent(original);

        assertThrows(IllegalArgumentException.class,
                () -> service.createEvent(null));

        assertEquals(List.of(original), service.getAllEvents());
    }

    @Test
    void getEventFindsMatchingIdAmongMultipleEvents() {
        Event first = event("event-1");
        Event second = event("event-2");
        service.createEvent(first);
        service.createEvent(second);

        assertSame(second, service.getEvent(new String("event-2")));
    }

    @Test
    void getEventReturnsNullWhenServiceIsEmpty() {
        assertNull(service.getEvent("missing"));
    }

    @Test
    void getEventReturnsNullForUnknownId() {
        service.createEvent(event("event-1"));

        assertNull(service.getEvent("missing"));
    }

    @Test
    void getEventReturnsNullForNullLookupId() {
        service.createEvent(event("event-1"));

        assertNull(service.getEvent(null));
    }

    @Test
    void getAllEventsReturnsEmptyListInitially() {
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    void getAllEventsReturnsEveryEventInCreationOrder() {
        Event first = event("event-1");
        Event second = event("event-2");
        Event third = event("event-3");
        service.createEvent(first);
        service.createEvent(second);
        service.createEvent(third);

        assertEquals(List.of(first, second, third), service.getAllEvents());
    }

    @Test
    void getAllEventsReturnsUnmodifiableList() {
        Event original = event("event-1");
        service.createEvent(original);
        List<Event> events = service.getAllEvents();

        assertThrows(UnsupportedOperationException.class,
                () -> events.add(event("event-2")));
        assertThrows(UnsupportedOperationException.class, events::clear);
        assertEquals(List.of(original), service.getAllEvents());
    }

    @Test
    void getAllEventsReturnsSnapshotUnaffectedByLaterCreation() {
        Event first = event("event-1");
        Event second = event("event-2");
        service.createEvent(first);
        List<Event> snapshot = service.getAllEvents();

        service.createEvent(second);

        assertEquals(List.of(first), snapshot);
        assertEquals(List.of(first, second), service.getAllEvents());
    }

    private Event event(String id) {
        return new Event(id, "Planning workshop", "Workshop", "Toronto",
                LocalDateTime.of(2026, 12, 1, 10, 0), 30, 1500.0);
    }

    @Test
    void deleteExistingEvent() {
        Event event = event("event-1");

        service.createEvent(event);

        assertTrue(service.deleteEvent("event-1"));
        assertNull(service.getEvent("event-1"));
        assertTrue(service.getAllEvents().isEmpty());
    }

    @Test
    void deleteNonExistentEvent() {
        assertFalse(service.deleteEvent("UNKNOWN"));
    }

    @Test
    void deleteEventPreservesOtherEvents() {
        Event first = event("event-1");
        Event second = event("event-2");

        service.createEvent(first);
        service.createEvent(second);

        assertTrue(service.deleteEvent("event-1"));

        assertNull(service.getEvent("event-1"));
        assertSame(second, service.getEvent("event-2"));
        assertEquals(List.of(second), service.getAllEvents());
    }
}