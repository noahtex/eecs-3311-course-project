package com.eventplanner.tool;

import com.eventplanner.ai.PlanningException;
import com.eventplanner.model.*;
import com.fasterxml.jackson.databind.*;
import java.net.URI;
import java.net.http.*;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;

/** Real Places Text Search (New) transport. Never log request headers, keys or response bodies. */
public final class PlacesAPIClient {
    public record Response(int status, String body) { }
    @FunctionalInterface public interface Transport { Response send(HttpRequest request) throws IOException, InterruptedException; }
    private final Supplier<String> apiKey;
    private final Transport transport;
    private final ObjectMapper json = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    public PlacesAPIClient() {
        this(PlacesConfiguration::loadApiKey, request -> {
            try (var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()) {
                var response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return new Response(response.statusCode(), response.body());
            }
        });
    }
    public PlacesAPIClient(Supplier<String> apiKey, Transport transport) {
        this.apiKey = Objects.requireNonNull(apiKey); this.transport = Objects.requireNonNull(transport);
    }
    public ToolResult searchPlaces(String query) {
        query = PlanningValues.text(query, "Places query");
        String key = apiKey.get(); // Configuration errors are already actionable and sanitized.
        try {
            String body = json.createObjectNode().put("textQuery", query).put("pageSize", 10).toString();
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://places.googleapis.com/v1/places:searchText"))
                    .timeout(Duration.ofSeconds(30)).header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", key)
                    .header("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress,places.googleMapsUri,places.businessStatus,places.types,places.accessibilityOptions")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            Response response = transport.send(request);
            if (response.status() < 200 || response.status() >= 300)
                throw new PlanningException("Places request failed (HTTP " + response.status()
                        + "). Check API key restrictions, Places API (New), billing, quota and network; retry or use venues add.");
            return parse(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PlanningException("Places request interrupted. Retry or use venues add.");
        } catch (PlanningException ex) { throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new PlanningException("Places request or response failed. Check network and provider availability; retry or use venues add.");
        }
    }
    private ToolResult parse(String body) throws IOException {
        JsonNode root = json.readTree(body);
        if (root == null || !root.isObject() || root.has("error") || (root.has("places") && !root.path("places").isArray()))
            throw new IllegalArgumentException("Malformed Places response.");
        List<VenueOption> results = new ArrayList<>();
        for (JsonNode place : root.path("places")) {
            String id = required(place.path("id"));
            String name = required(place.path("displayName").path("text"));
            String address = place.path("formattedAddress").isTextual() && !place.path("formattedAddress").asText().isBlank()
                    ? place.path("formattedAddress").asText() : "Address not supplied by provider";
            Map<String, String> attributes = new LinkedHashMap<>();
            for (String field : List.of("googleMapsUri", "businessStatus")) {
                if (place.path(field).isTextual()) attributes.put(field, place.path(field).textValue());
            }
            if (place.path("types").isArray()) attributes.put("types", place.path("types").toString());
            if (place.path("accessibilityOptions").isObject()) attributes.put("accessibilityOptions", place.path("accessibilityOptions").toString());
            var entrance = place.path("accessibilityOptions").path("wheelchairAccessibleEntrance");
            Evidence<Boolean> access = entrance.isBoolean()
                    ? new Evidence<>(entrance.booleanValue(), Evidence.Status.PROVIDER_REPORTED, "Google Places accessibilityOptions.wheelchairAccessibleEntrance")
                    : Evidence.unknown();
            results.add(new VenueOption("google:" + id, name, address, "Google Places", Instant.now(), attributes,
                    Evidence.unknown(), Evidence.unknown(), "CAD", access, Evidence.unknown()));
        }
        return new ToolResult("Google Places", results, List.of(
                "Provider-reported place details are not independently verified. Event availability, capacity, rental cost, equipment and dietary suitability are unknown.",
                "Up to 10 candidates from the first response; results are not an exhaustive venue inventory."));
    }
    private String required(JsonNode value) {
        if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException("Missing provider identity.");
        return value.textValue();
    }
}
