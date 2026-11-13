package com.eventplanner.tool;

import com.eventplanner.ai.PlanningException;
import com.eventplanner.model.Evidence;
import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class PlacesAPIClientTest {
    @TempDir Path directory;
    @Test void loadsLocalConfigurationIndependentlyOfGeminiWithEnvironmentPrecedence() throws Exception {
        Path path = directory.resolve(".env");
        Files.writeString(path, "\uFEFF# test only\nexport GOOGLE_PLACES_API_KEY='fake-local'\n");
        assertEquals("fake-local", PlacesConfiguration.loadApiKey(path, Map.of()));
        assertEquals("fake-environment", PlacesConfiguration.loadApiKey(path, Map.of("GOOGLE_PLACES_API_KEY", "fake-environment")));
        assertThrows(PlanningException.class, () -> PlacesConfiguration.loadApiKey(path, Map.of("GOOGLE_PLACES_API_KEY", " ")));
    }
    @Test void missingConfigurationFailsBeforeTransportWithManualEntryGuidance() {
        var client = new PlacesAPIClient(() -> PlacesConfiguration.loadApiKey(directory.resolve("absent"), Map.of()), request -> {
            fail("No external request should occur"); return null;
        });
        var failure = assertThrows(PlanningException.class, () -> client.searchPlaces("event venue"));
        assertTrue(failure.getMessage().contains("GOOGLE_PLACES_API_KEY"));
        assertTrue(failure.getMessage().contains("venues add"));
    }
    @Test void mapsActualProviderFieldsWithoutInferringPricesCapacityOrOtherAccessibility() {
        var client = new PlacesAPIClient(() -> "fake-test-key", request -> {
            assertEquals("https://places.googleapis.com/v1/places:searchText", request.uri().toString());
            assertEquals("POST", request.method());
            String mask=request.headers().firstValue("X-Goog-FieldMask").orElseThrow();
            assertTrue(mask.contains("places.id"));
            assertTrue(mask.contains("places.nationalPhoneNumber"));
            assertTrue(mask.contains("places.websiteUri"));
            assertFalse(mask.contains("*"));
            return new PlacesAPIClient.Response(200, """
                {"places":[{"id":"actual-provider-id","displayName":{"text":"Provider-returned name"},
                "formattedAddress":"Provider-returned address","googleMapsUri":"https://maps.google.com/?cid=1",
                "types":["event_venue"],"businessStatus":"OPERATIONAL",
                "nationalPhoneNumber":"(416) 555-0100","websiteUri":"https://example.org/venue","email":"not-a-places-field@example.org",
                "accessibilityOptions":{"wheelchairAccessibleEntrance":false,"wheelchairAccessibleParking":true}}]}
                """);
        });
        var result = new VenueSearchTool(client).execute("venues near campus");
        var venue = result.options().get(0);
        assertEquals("google:actual-provider-id", venue.id());
        assertEquals("Provider-returned name", venue.name());
        assertEquals("Provider-returned address", venue.address());
        assertEquals("(416) 555-0100",venue.providerAttributes().get("nationalPhoneNumber"));
        assertEquals("https://example.org/venue",venue.providerAttributes().get("websiteUri"));
        assertFalse(venue.providerAttributes().containsKey("email"));
        assertEquals(Evidence.Status.PROVIDER_REPORTED, venue.wheelchairEntrance().status());
        assertFalse(venue.wheelchairEntrance().value());
        assertNull(venue.capacity().value()); assertNull(venue.rentalCost().value()); assertNull(venue.equipment().value());
        assertTrue(venue.providerAttributes().get("accessibilityOptions").contains("wheelchairAccessibleParking"));
        assertFalse(result.warnings().isEmpty());
        assertEquals(1, new CateringSearchTool(client).execute("catering").options().size());
    }
    @Test void emptyProviderResultsStayEmptyAndMissingFieldsStayUnknown() {
        var empty = new PlacesAPIClient(() -> "fake", request -> new PlacesAPIClient.Response(200, "{}"));
        assertTrue(empty.searchPlaces("nothing").options().isEmpty());
        var missing = new PlacesAPIClient(() -> "fake", request -> new PlacesAPIClient.Response(200,
                "{\"places\":[{\"id\":\"p\",\"displayName\":{\"text\":\"N\"}}]}"));
        var venue=missing.searchPlaces("query").options().get(0);
        assertEquals(Evidence.Status.UNKNOWN, venue.wheelchairEntrance().status());
        assertFalse(venue.providerAttributes().containsKey("nationalPhoneNumber"));
        assertFalse(venue.providerAttributes().containsKey("websiteUri"));
    }
    @Test void httpTransportAndMalformedResponseFailuresNeverEchoRawData() {
        for (int status : new int[]{400, 403, 429, 500}) {
            var client = new PlacesAPIClient(() -> "fake", request -> new PlacesAPIClient.Response(status, "private-provider-response key=fake"));
            var error = assertThrows(PlanningException.class, () -> client.searchPlaces("query"));
            assertTrue(error.getMessage().contains("HTTP " + status)); assertFalse(error.getMessage().contains("private-provider"));
            assertNull(error.getCause());
        }
        var network = new PlacesAPIClient(() -> "fake", request -> { throw new IOException("private-header key=fake"); });
        var error = assertThrows(PlanningException.class, () -> network.searchPlaces("query"));
        assertFalse(error.getMessage().contains("private-header")); assertNull(error.getCause());
        for (String invalid : new String[]{"private-provider-response", "null", "{\"places\":42}", "{\"places\":[{}]}", "{} {}"}) {
            var malformed = new PlacesAPIClient(() -> "fake", request -> new PlacesAPIClient.Response(200, invalid));
            assertThrows(PlanningException.class, () -> malformed.searchPlaces("query"));
        }
    }
    @Test void interruptionIsPreservedWithoutExposingDetails() {
        var client = new PlacesAPIClient(() -> "fake", request -> { throw new InterruptedException("private"); });
        try {
            assertThrows(PlanningException.class, () -> client.searchPlaces("query"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally { Thread.interrupted(); }
    }
}
