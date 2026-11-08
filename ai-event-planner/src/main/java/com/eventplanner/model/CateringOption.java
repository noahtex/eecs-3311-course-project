package com.eventplanner.model;

import java.util.List;

/** AI recommendation, not a supplier quote or verified allergen guarantee. */
public record CateringOption(String providerId, String recommendation, List<String> dietaryNotes) {
    public CateringOption {
        if (providerId != null) providerId = PlanningValues.text(providerId, "Provider ID");
        recommendation = PlanningValues.text(recommendation, "Catering recommendation");
        dietaryNotes = PlanningValues.texts(dietaryNotes, "Dietary notes", false);
    }
}
