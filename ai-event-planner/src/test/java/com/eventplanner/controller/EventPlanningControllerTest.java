package com.eventplanner.controller;

import com.eventplanner.facade.EventPlanningFacade;
import com.eventplanner.service.EventService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventPlanningControllerTest {
    @Test
    void delegatesWorkflowAndValidationToFacade() {
        var facade = new EventPlanningFacade(new EventService());
        var controller = new EventPlanningController(facade);
        var event = controller.createEvent("Workshop", "Club", "Campus", LocalDateTime.now(), 10, 20);
        assertSame(event, facade.viewEvent(event.getId()));
        assertSame(event, controller.viewEvent(event.getId()));
        assertEquals(java.util.List.of(event), controller.listEvents());
        assertThrows(IllegalArgumentException.class, () -> controller.createEvent("", "Club", "Campus", LocalDateTime.now(), 10, 20));
        assertTrue(controller.deleteEvent(event.getId()));
        assertFalse(controller.deleteEvent(event.getId()));
        assertThrows(IllegalArgumentException.class, () -> controller.viewEvent(event.getId()));
        assertTrue(controller.listEvents().isEmpty());
    }
}
