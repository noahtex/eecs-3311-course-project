package com.eventplanner.model;

import java.util.List;

/** Score/warnings are deterministic; narrative and trade-offs are explicitly AI interpretation. */
public record VenueComparison(String venueId, int knownFitScore, List<String> warnings,
        String analysis, List<String> tradeOffs) {
    public VenueComparison {
        venueId = PlanningValues.text(venueId, "Venue ID");
        warnings = PlanningValues.texts(warnings, "Venue warnings", true);
        analysis = PlanningValues.text(analysis, "Venue analysis");
        tradeOffs = PlanningValues.texts(tradeOffs, "Trade-offs", false);
    }
}
