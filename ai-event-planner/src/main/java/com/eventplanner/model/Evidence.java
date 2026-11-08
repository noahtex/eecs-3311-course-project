package com.eventplanner.model;

import java.util.Objects;

/** Provenance travels with each attribute; missing data is never inferred from another field. */
public record Evidence<T>(T value, Status status, String source) {
    public enum Status { PROVIDER_REPORTED, PLANNER_CONFIRMED, ESTIMATED, UNKNOWN }
    public Evidence {
        Objects.requireNonNull(status, "Evidence status is required.");
        source = PlanningValues.text(source, "Evidence source");
        if ((status == Status.UNKNOWN) != (value == null)) throw new IllegalArgumentException("Unknown evidence must have no value.");
    }
    public static <T> Evidence<T> unknown() { return new Evidence<>(null, Status.UNKNOWN, "Not supplied"); }
}
