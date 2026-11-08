package com.eventplanner.service;

import com.eventplanner.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.eventplanner.support.CommercialFixtures.*;
import static com.eventplanner.support.ExecutionFixtures.event;

class VenueServiceTest {
    private final VenueService service = new VenueService();
    @Test void ranksKnownFitAheadOfUnknownWithoutClaimingAvailability() {
        var event = event(); event.configureVenueNeeds(true, List.of("projector"));
        var known = service.evaluate(event, knownVenue("known"));
        var unknown = service.evaluate(event, unknownVenue("unknown"));
        assertTrue(known.knownFitScore() > unknown.knownFitScore());
        assertEquals("known", service.rank(List.of(unknown, known)).get(0).venueId());
        assertTrue(unknown.warnings().stream().anyMatch(w -> w.contains("Capacity unknown")));
        assertTrue(unknown.warnings().stream().anyMatch(w -> w.contains("Rental cost unknown")));
        assertTrue(known.warnings().stream().anyMatch(w -> w.contains("availability is UNKNOWN")));
    }
    @Test void flagsKnownCapacityCostAccessibilityAndEquipmentFailures() {
        var event = event(); event.configureVenueNeeds(true, List.of("projector"));
        var venue = new VenueOption("bad", "Test", "Far away", "Test source", java.time.Instant.EPOCH, Map.of(),
                confirmed(5), confirmed(amount("600")), "CAD", confirmed(false), confirmed(List.of()));
        var comparison = service.evaluate(event, venue);
        for (String fragment : List.of("below expected", "exceeds", "does not meet", "equipment not listed", "location preference"))
            assertTrue(comparison.warnings().stream().anyMatch(w -> w.contains(fragment)), fragment);
        assertTrue(comparison.knownFitScore() < 0);
    }
    @Test void estimatedAttributesDoNotEarnConfirmedFitPoints() {
        var event = event(); event.configureVenueNeeds(true, List.of("projector"));
        var estimate = new VenueOption("estimate", "Test", "Far away", "Estimate", java.time.Instant.EPOCH, Map.of(),
                new Evidence<>(100, Evidence.Status.ESTIMATED, "Guess"), new Evidence<>(amount("1"), Evidence.Status.ESTIMATED, "Guess"),
                "CAD", new Evidence<>(true, Evidence.Status.ESTIMATED, "Guess"), new Evidence<>(List.of("projector"), Evidence.Status.ESTIMATED, "Guess"));
        assertEquals(0, service.evaluate(event, estimate).knownFitScore());
        assertThrows(IllegalArgumentException.class, () -> new Evidence<>(100, Evidence.Status.UNKNOWN, "Not known"));
    }
}
