package com.eventplanner.model;

import java.util.List;

public record ToolResult(String source, List<VenueOption> options, List<String> warnings) {
    public ToolResult {
        source = PlanningValues.text(source, "Tool source");
        options = List.copyOf(options);
        warnings = PlanningValues.texts(warnings, "Tool warnings", true);
        if (options.stream().map(VenueOption::id).distinct().count() != options.size())
            throw new IllegalArgumentException("Provider option IDs must be unique.");
    }
}
