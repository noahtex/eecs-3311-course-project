package com.eventplanner.tool;

import com.eventplanner.model.ToolResult;
import java.util.Objects;

public final class VenueSearchTool implements AITool {
    private final PlacesAPIClient client;
    public VenueSearchTool(PlacesAPIClient client) { this.client = Objects.requireNonNull(client); }
    @Override public ToolResult execute(String query) { return client.searchPlaces(query); }
    @Override public void close() { client.close(); }
}
