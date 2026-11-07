package com.eventplanner.model;

import java.util.List;

/** Shared domain validation; never accepts missing model fields silently. */
public final class PlanningValues {
    private PlanningValues() { }

    public static String text(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required.");
        return value.trim();
    }

    public static List<String> texts(List<String> values, String field, boolean allowEmpty) {
        if (values == null || (!allowEmpty && values.isEmpty())) {
            throw new IllegalArgumentException(field + " must be a " + (allowEmpty ? "list." : "non-empty list."));
        }
        return values.stream().map(value -> text(value, field)).toList();
    }
}
