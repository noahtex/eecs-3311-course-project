package com.eventplanner.tool;

import com.eventplanner.model.ToolResult;
import java.util.Objects;

/** Provider discovery only: Places supplies neither event menus nor catering quotes. */
public final class CateringSearchTool implements AITool {
    private final PlacesAPIClient client;
    public CateringSearchTool(PlacesAPIClient client) { this.client = Objects.requireNonNull(client); }
    @Override public ToolResult execute(String query) { return client.searchPlaces(query); }
}
